#include <jni.h>

#include "llama.h"

#include <algorithm>
#include <cmath>
#include <limits>
#include <mutex>
#include <stdexcept>
#include <string>
#include <vector>

namespace {
std::mutex runtime_mutex;
llama_model * model = nullptr;
llama_context * context = nullptr;

void throw_illegal_state(JNIEnv * env, const char * message) {
    const jclass type = env->FindClass("java/lang/IllegalStateException");
    if (type != nullptr) env->ThrowNew(type, message);
}

std::string to_string(JNIEnv * env, jstring value) {
    if (value == nullptr) return {};
    const char * chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

std::vector<llama_token> tokenize(const llama_vocab * vocab, const std::string & text) {
    int32_t count = llama_tokenize(vocab, text.data(), static_cast<int32_t>(text.size()),
                                   nullptr, 0, true, true);
    if (count == 0) return {};
    if (count > 0) throw std::runtime_error("Unexpected tokenizer size response.");
    std::vector<llama_token> tokens(static_cast<size_t>(-count));
    count = llama_tokenize(vocab, text.data(), static_cast<int32_t>(text.size()),
                           tokens.data(), static_cast<int32_t>(tokens.size()), true, true);
    if (count < 0) throw std::runtime_error("Tokenization failed.");
    tokens.resize(static_cast<size_t>(count));
    return tokens;
}

float token_log_probability(const float * logits, int32_t vocabulary_size, llama_token target) {
    const float maximum = *std::max_element(logits, logits + vocabulary_size);
    double sum = 0.0;
    for (int32_t i = 0; i < vocabulary_size; ++i) {
        sum += std::exp(static_cast<double>(logits[i] - maximum));
    }
    return logits[target] - maximum - static_cast<float>(std::log(sum));
}

float score_candidate(const std::string & conditioning, const std::string & candidate) {
    const llama_vocab * vocab = llama_model_get_vocab(model);
    const auto conditioning_tokens = tokenize(vocab, conditioning);
    const auto tokens = tokenize(vocab, conditioning + candidate);
    if (tokens.size() < 2 || tokens.size() <= conditioning_tokens.size()) {
        throw std::runtime_error("Candidate has no scoreable tokens.");
    }

    llama_memory_clear(llama_get_memory(context), true);
    const int32_t vocabulary_size = llama_vocab_n_tokens(vocab);
    double total = 0.0;
    int32_t scored = 0;
    for (size_t index = 0; index + 1 < tokens.size(); ++index) {
        llama_token current = tokens[index];
        if (llama_decode(context, llama_batch_get_one(&current, 1)) != 0) {
            throw std::runtime_error("Model decode failed.");
        }
        if (index + 1 >= conditioning_tokens.size()) {
            const float * logits = llama_get_logits(context);
            if (logits == nullptr) throw std::runtime_error("Model returned no logits.");
            total += token_log_probability(logits, vocabulary_size, tokens[index + 1]);
            ++scored;
        }
    }
    if (scored == 0) throw std::runtime_error("Candidate has no scored suffix.");
    return static_cast<float>(total / scored);
}

std::string token_piece(const llama_vocab * vocab, llama_token token) {
    std::vector<char> buffer(128);
    int32_t count = llama_token_to_piece(vocab, token, buffer.data(),
                                         static_cast<int32_t>(buffer.size()), 0, true);
    if (count < 0) {
        buffer.resize(static_cast<size_t>(-count));
        count = llama_token_to_piece(vocab, token, buffer.data(),
                                     static_cast<int32_t>(buffer.size()), 0, true);
    }
    if (count < 0) throw std::runtime_error("Could not decode generated token.");
    return std::string(buffer.data(), static_cast<size_t>(count));
}

std::string generate_candidate(
        const std::string & conditioning,
        const std::string & prefix,
        int32_t max_tokens,
        uint32_t seed) {
    const llama_vocab * vocab = llama_model_get_vocab(model);
    const auto prompt_tokens = tokenize(vocab, conditioning + prefix);
    if (prompt_tokens.empty()) throw std::runtime_error("Generation prompt is empty.");

    llama_memory_clear(llama_get_memory(context), true);
    if (llama_decode(context, llama_batch_get_one(
            const_cast<llama_token *>(prompt_tokens.data()),
            static_cast<int32_t>(prompt_tokens.size()))) != 0) {
        throw std::runtime_error("Generation prompt decode failed.");
    }

    auto sampler_params = llama_sampler_chain_default_params();
    sampler_params.no_perf = true;
    llama_sampler * sampler = llama_sampler_chain_init(sampler_params);
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.90f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.75f));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(seed));

    std::string result = prefix;
    for (int32_t index = 0; index < max_tokens; ++index) {
        const llama_token token = llama_sampler_sample(sampler, context, -1);
        if (llama_vocab_is_eog(vocab, token)) break;
        const std::string piece = token_piece(vocab, token);
        if (piece.find('\n') != std::string::npos || piece.find('\r') != std::string::npos) break;
        result += piece;
        llama_token mutable_token = token;
        if (llama_decode(context, llama_batch_get_one(&mutable_token, 1)) != 0) {
            llama_sampler_free(sampler);
            throw std::runtime_error("Generated token decode failed.");
        }
    }
    llama_sampler_free(sampler);
    return result;
}

void unload_locked() {
    if (context != nullptr) {
        llama_free(context);
        context = nullptr;
    }
    if (model != nullptr) {
        llama_model_free(model);
        model = nullptr;
    }
}
} // namespace

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *, void *) {
    llama_backend_init();
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *, void *) {
    std::lock_guard<std::mutex> lock(runtime_mutex);
    unload_locked();
    llama_backend_free();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_finclue_sdk_prediction_LlamaNativeRuntime_nativeRuntimeVersion(JNIEnv * env, jobject) {
    return env->NewStringUTF(llama_print_system_info());
}

extern "C" JNIEXPORT void JNICALL
Java_com_finclue_sdk_prediction_LlamaNativeRuntime_loadModel(
        JNIEnv * env, jobject, jstring model_path, jint context_size, jint thread_count) {
    std::lock_guard<std::mutex> lock(runtime_mutex);
    unload_locked();
    const std::string path = to_string(env, model_path);
    if (path.empty() || context_size <= 0 || thread_count <= 0) {
        throw_illegal_state(env, "Invalid model configuration.");
        return;
    }

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;
    model = llama_model_load_from_file(path.c_str(), model_params);
    if (model == nullptr) {
        throw_illegal_state(env, "Could not load bundled prediction model.");
        return;
    }

    llama_context_params context_params = llama_context_default_params();
    context_params.n_ctx = static_cast<uint32_t>(context_size);
    context_params.n_batch = static_cast<uint32_t>(context_size);
    context_params.n_ubatch = static_cast<uint32_t>(context_size);
    context_params.n_threads = thread_count;
    context_params.n_threads_batch = thread_count;
    context_params.no_perf = true;
    context = llama_init_from_model(model, context_params);
    if (context == nullptr) {
        unload_locked();
        throw_illegal_state(env, "Could not create prediction model context.");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_finclue_sdk_prediction_LlamaNativeRuntime_unloadModel(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(runtime_mutex);
    unload_locked();
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_finclue_sdk_prediction_LlamaNativeRuntime_scoreCandidates(
        JNIEnv * env, jobject, jstring conditioning_text, jobjectArray candidates) {
    std::lock_guard<std::mutex> lock(runtime_mutex);
    if (model == nullptr || context == nullptr) {
        throw_illegal_state(env, "Prediction model is not loaded.");
        return nullptr;
    }
    if (candidates == nullptr) {
        throw_illegal_state(env, "Candidates must not be null.");
        return nullptr;
    }

    try {
        const std::string conditioning = to_string(env, conditioning_text);
        const jsize count = env->GetArrayLength(candidates);
        std::vector<jfloat> scores(static_cast<size_t>(count));
        for (jsize i = 0; i < count; ++i) {
            auto value = static_cast<jstring>(env->GetObjectArrayElement(candidates, i));
            const std::string candidate = to_string(env, value);
            env->DeleteLocalRef(value);
            scores[static_cast<size_t>(i)] = score_candidate(conditioning, candidate);
        }
        jfloatArray result = env->NewFloatArray(count);
        if (result != nullptr && count > 0) {
            env->SetFloatArrayRegion(result, 0, count, scores.data());
        }
        return result;
    } catch (const std::exception & error) {
        throw_illegal_state(env, error.what());
        return nullptr;
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_finclue_sdk_prediction_LlamaNativeRuntime_generateCandidates(
        JNIEnv * env,
        jobject,
        jstring conditioning_text,
        jstring prefix_text,
        jint candidate_count,
        jint max_tokens) {
    std::lock_guard<std::mutex> lock(runtime_mutex);
    if (model == nullptr || context == nullptr) {
        throw_illegal_state(env, "Prediction model is not loaded.");
        return nullptr;
    }
    if (candidate_count <= 0 || candidate_count > 32 || max_tokens <= 0 || max_tokens > 32) {
        throw_illegal_state(env, "Invalid generation limits.");
        return nullptr;
    }

    try {
        const std::string conditioning = to_string(env, conditioning_text);
        const std::string prefix = to_string(env, prefix_text);
        const jclass string_class = env->FindClass("java/lang/String");
        jobjectArray result = env->NewObjectArray(candidate_count, string_class, nullptr);
        for (jint index = 0; index < candidate_count; ++index) {
            const std::string candidate = generate_candidate(
                conditioning,
                prefix,
                max_tokens,
                0xF1C1u + static_cast<uint32_t>(index) * 7919u);
            jstring value = env->NewStringUTF(candidate.c_str());
            env->SetObjectArrayElement(result, index, value);
            env->DeleteLocalRef(value);
        }
        return result;
    } catch (const std::exception & error) {
        throw_illegal_state(env, error.what());
        return nullptr;
    }
}
