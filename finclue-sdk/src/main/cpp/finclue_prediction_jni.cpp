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
constexpr int32_t MAX_PARALLEL_CANDIDATES = 6;
constexpr int32_t MAX_SKIPPED_INITIAL_TOKENS = 8;

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

jstring to_java_string(JNIEnv * env, const std::string & utf8) {
    std::vector<jchar> utf16;
    utf16.reserve(utf8.size());
    for (size_t index = 0; index < utf8.size();) {
        const auto first = static_cast<unsigned char>(utf8[index]);
        uint32_t code_point = 0;
        size_t length = 0;
        uint32_t minimum = 0;
        if (first < 0x80) {
            code_point = first;
            length = 1;
        } else if ((first & 0xE0) == 0xC0) {
            code_point = first & 0x1F;
            length = 2;
            minimum = 0x80;
        } else if ((first & 0xF0) == 0xE0) {
            code_point = first & 0x0F;
            length = 3;
            minimum = 0x800;
        } else if ((first & 0xF8) == 0xF0) {
            code_point = first & 0x07;
            length = 4;
            minimum = 0x10000;
        } else {
            utf16.push_back(0xFFFD);
            ++index;
            continue;
        }

        bool valid = index + length <= utf8.size();
        for (size_t offset = 1; valid && offset < length; ++offset) {
            const auto continuation = static_cast<unsigned char>(utf8[index + offset]);
            if ((continuation & 0xC0) != 0x80) {
                valid = false;
            } else {
                code_point = (code_point << 6) | (continuation & 0x3F);
            }
        }
        valid = valid && code_point >= minimum && code_point <= 0x10FFFF &&
                !(code_point >= 0xD800 && code_point <= 0xDFFF);
        if (!valid) {
            utf16.push_back(0xFFFD);
            ++index;
            continue;
        }
        index += length;
        if (code_point <= 0xFFFF) {
            utf16.push_back(static_cast<jchar>(code_point));
        } else {
            code_point -= 0x10000;
            utf16.push_back(static_cast<jchar>(0xD800 + (code_point >> 10)));
            utf16.push_back(static_cast<jchar>(0xDC00 + (code_point & 0x3FF)));
        }
    }
    return env->NewString(utf16.data(), static_cast<jsize>(utf16.size()));
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

std::vector<llama_token> highest_probability_tokens(
        const float * logits,
        const llama_vocab * vocab,
        int32_t count) {
    const int32_t vocabulary_size = llama_vocab_n_tokens(vocab);
    std::vector<llama_token> tokens(static_cast<size_t>(vocabulary_size));
    for (int32_t index = 0; index < vocabulary_size; ++index) {
        tokens[static_cast<size_t>(index)] = index;
    }
    const auto compare = [logits](llama_token left, llama_token right) {
        return logits[left] > logits[right];
    };
    const size_t inspected = std::min(
            tokens.size(),
            static_cast<size_t>(count + MAX_SKIPPED_INITIAL_TOKENS));
    std::partial_sort(tokens.begin(), tokens.begin() + inspected, tokens.end(), compare);

    std::vector<llama_token> result;
    result.reserve(static_cast<size_t>(count));
    for (size_t index = 0; index < inspected && result.size() < static_cast<size_t>(count); ++index) {
        const llama_token token = tokens[index];
        if (!llama_vocab_is_eog(vocab, token)) result.push_back(token);
    }
    return result;
}

llama_token highest_probability_token(const float * logits, const llama_vocab * vocab) {
    return highest_probability_tokens(logits, vocab, 1).front();
}

void clear_batch(llama_batch & batch) {
    batch.n_tokens = 0;
}

void add_batch_token(
        llama_batch & batch,
        llama_token token,
        llama_pos position,
        llama_seq_id sequence,
        bool request_logits) {
    const int32_t index = batch.n_tokens;
    batch.token[index] = token;
    batch.pos[index] = position;
    batch.n_seq_id[index] = 1;
    batch.seq_id[index][0] = sequence;
    batch.logits[index] = request_logits ? 1 : 0;
    ++batch.n_tokens;
}

std::vector<std::string> generate_parallel_candidates(
        const std::vector<llama_token> & prompt_tokens,
        const std::string & prefix,
        int32_t candidate_count,
        int32_t max_tokens) {
    const llama_vocab * vocab = llama_model_get_vocab(model);
    llama_memory_t memory = llama_get_memory(context);
    llama_memory_clear(memory, true);
    if (llama_decode(context, llama_batch_get_one(
            const_cast<llama_token *>(prompt_tokens.data()),
            static_cast<int32_t>(prompt_tokens.size()))) != 0) {
        throw std::runtime_error("Generation prompt decode failed.");
    }

    const float * prompt_logits = llama_get_logits_ith(context, -1);
    if (prompt_logits == nullptr) throw std::runtime_error("Generation prompt returned no logits.");
    const auto first_tokens = highest_probability_tokens(prompt_logits, vocab, candidate_count);
    if (first_tokens.empty()) return {};

    const int32_t beam_count = static_cast<int32_t>(first_tokens.size());
    for (int32_t sequence = 1; sequence < beam_count; ++sequence) {
        llama_memory_seq_cp(memory, 0, sequence, -1, -1);
    }

    llama_batch batch = llama_batch_init(beam_count, 0, 1);
    std::vector<std::string> results(static_cast<size_t>(beam_count), prefix);
    std::vector<int32_t> logit_rows(static_cast<size_t>(beam_count), -1);
    std::vector<bool> active(static_cast<size_t>(beam_count), true);
    try {
        for (int32_t sequence = 0; sequence < beam_count; ++sequence) {
            const llama_token token = first_tokens[static_cast<size_t>(sequence)];
            const std::string piece = token_piece(vocab, token);
            if (piece.find('\n') != std::string::npos || piece.find('\r') != std::string::npos) {
                active[static_cast<size_t>(sequence)] = false;
                continue;
            }
            results[static_cast<size_t>(sequence)] += piece;
            logit_rows[static_cast<size_t>(sequence)] = batch.n_tokens;
            add_batch_token(
                    batch,
                    token,
                    static_cast<llama_pos>(prompt_tokens.size()),
                    sequence,
                    true);
        }
        if (batch.n_tokens > 0 && llama_decode(context, batch) != 0) {
            throw std::runtime_error("Initial parallel candidate decode failed.");
        }

        for (int32_t step = 1; step < max_tokens; ++step) {
            clear_batch(batch);
            for (int32_t sequence = 0; sequence < beam_count; ++sequence) {
                if (!active[static_cast<size_t>(sequence)]) continue;
                const int32_t row = logit_rows[static_cast<size_t>(sequence)];
                const float * logits = llama_get_logits_ith(context, row);
                if (logits == nullptr) throw std::runtime_error("Parallel candidate returned no logits.");
                const llama_token token = highest_probability_token(logits, vocab);
                if (llama_vocab_is_eog(vocab, token)) {
                    active[static_cast<size_t>(sequence)] = false;
                    continue;
                }
                const std::string piece = token_piece(vocab, token);
                if (piece.find('\n') != std::string::npos || piece.find('\r') != std::string::npos) {
                    active[static_cast<size_t>(sequence)] = false;
                    continue;
                }
                results[static_cast<size_t>(sequence)] += piece;
                logit_rows[static_cast<size_t>(sequence)] = batch.n_tokens;
                add_batch_token(
                        batch,
                        token,
                        static_cast<llama_pos>(prompt_tokens.size() + step),
                        sequence,
                        true);
            }
            if (batch.n_tokens == 0) break;
            if (llama_decode(context, batch) != 0) {
                throw std::runtime_error("Parallel candidate decode failed.");
            }
        }
        llama_batch_free(batch);
        return results;
    } catch (...) {
        llama_batch_free(batch);
        throw;
    }
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
    context_params.n_seq_max = MAX_PARALLEL_CANDIDATES;
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
        jint max_tokens,
        jint) {
    std::lock_guard<std::mutex> lock(runtime_mutex);
    if (model == nullptr || context == nullptr) {
        throw_illegal_state(env, "Prediction model is not loaded.");
        return nullptr;
    }
    if (candidate_count <= 0 || candidate_count > MAX_PARALLEL_CANDIDATES ||
        max_tokens <= 0 || max_tokens > 32) {
        throw_illegal_state(env, "Invalid generation limits.");
        return nullptr;
    }

    try {
        const std::string conditioning = to_string(env, conditioning_text);
        const std::string prefix = to_string(env, prefix_text);
        const llama_vocab * vocab = llama_model_get_vocab(model);
        const auto prompt_tokens = tokenize(vocab, conditioning + prefix);
        if (prompt_tokens.empty()) throw std::runtime_error("Generation prompt is empty.");

        const auto candidates = generate_parallel_candidates(
                prompt_tokens,
                prefix,
                candidate_count,
                max_tokens);
        const jclass string_class = env->FindClass("java/lang/String");
        jobjectArray result = env->NewObjectArray(
                static_cast<jsize>(candidates.size()), string_class, nullptr);
        for (size_t index = 0; index < candidates.size(); ++index) {
            jstring value = to_java_string(env, candidates[index]);
            env->SetObjectArrayElement(result, index, value);
            env->DeleteLocalRef(value);
        }
        return result;
    } catch (const std::exception & error) {
        throw_illegal_state(env, error.what());
        return nullptr;
    }
}
