#include "server.h"

#include <algorithm>
#include <cmath>
#include <iostream>
#include <memory>
#include <string>
#include <vector>

#include <grpcpp/grpcpp.h>

grpc::Status SmaServiceImpl::ComputeSma(grpc::ServerContext* /*context*/,
                                       const indicator::SmaRequest* request,
                                       indicator::SmaResponse* response) {
    const int period = request->period();
    if (period <= 0) {
        return {grpc::StatusCode::INVALID_ARGUMENT, "period must be positive"};
    }

    const auto& values = request->values();
    const int n = values.size();
    auto* result = response->mutable_result();
    result->Reserve(n);

    const double nan = std::numeric_limits<double>::quiet_NaN();
    double window_sum = 0.0;

    for (int i = 0; i < n; ++i) {
        window_sum += values[i];
        if (i >= period) {
            window_sum -= values[i - period];
        }
        if (i >= period - 1) {
            result->Add(window_sum / period);
        } else {
            result->Add(nan);
        }
    }

    return grpc::Status::OK;
}

grpc::Status SmaServiceImpl::ComputeEma(grpc::ServerContext* /*context*/,
                                       const indicator::EmaRequest* request,
                                       indicator::EmaResponse* response) {
    const int period = request->period();
    if (period <= 0) {
        return {grpc::StatusCode::INVALID_ARGUMENT, "period must be positive"};
    }

    const auto& values = request->values();
    const int n = values.size();
    auto* result = response->mutable_result();
    result->Reserve(n);

    const double nan = std::numeric_limits<double>::quiet_NaN();
    const double k = 2.0 / (period + 1);

    double seed_sum = 0.0;
    double ema_prev = 0.0;

    for (int i = 0; i < n; ++i) {
        if (i < period - 1) {
            seed_sum += values[i];
            result->Add(nan);
        } else if (i == period - 1) {
            seed_sum += values[i];
            ema_prev = seed_sum / period;
            result->Add(ema_prev);
        } else {
            ema_prev = values[i] * k + ema_prev * (1.0 - k);
            result->Add(ema_prev);
        }
    }

    return grpc::Status::OK;
}

grpc::Status SmaServiceImpl::ComputeRsi(grpc::ServerContext* /*context*/,
                                       const indicator::RsiRequest* request,
                                       indicator::RsiResponse* response) {
    const int period = request->period();
    if (period <= 0) {
        return {grpc::StatusCode::INVALID_ARGUMENT, "period must be positive"};
    }

    const auto& values = request->values();
    const int n = values.size();
    auto* result = response->mutable_result();
    result->Reserve(n);

    const double nan = std::numeric_limits<double>::quiet_NaN();

    double avg_gain = 0.0;
    double avg_loss = 0.0;

    for (int i = 0; i < n; ++i) {
        if (i == 0) {
            result->Add(nan);
            continue;
        }

        const double delta = values[i] - values[i - 1];
        const double gain = delta > 0.0 ? delta : 0.0;
        const double loss = delta < 0.0 ? -delta : 0.0;

        if (i < period) {
            avg_gain += gain;
            avg_loss += loss;
            result->Add(nan);
        } else if (i == period) {
            avg_gain = (avg_gain + gain) / period;
            avg_loss = (avg_loss + loss) / period;
            if (avg_loss == 0.0) {
                result->Add(100.0);
            } else {
                const double rs = avg_gain / avg_loss;
                result->Add(100.0 - 100.0 / (1.0 + rs));
            }
        } else {
            avg_gain = (avg_gain * (period - 1) + gain) / period;
            avg_loss = (avg_loss * (period - 1) + loss) / period;
            if (avg_loss == 0.0) {
                result->Add(100.0);
            } else {
                const double rs = avg_gain / avg_loss;
                result->Add(100.0 - 100.0 / (1.0 + rs));
            }
        }
    }

    return grpc::Status::OK;
}

grpc::Status SmaServiceImpl::FindAnalogs(grpc::ServerContext* /*context*/,
                                         const indicator::FindAnalogsRequest* request,
                                         indicator::FindAnalogsResponse* response) {
    const auto& current = request->current();
    const auto& history = request->history();
    const int k = request->k();
    const int dim = current.size();

    if (dim <= 0) {
        return {grpc::StatusCode::INVALID_ARGUMENT, "current vector is empty"};
    }
    if (history.size() % dim != 0) {
        return {grpc::StatusCode::INVALID_ARGUMENT, "history size is not a multiple of dim"};
    }
    if (k <= 0) {
        return {grpc::StatusCode::INVALID_ARGUMENT, "k must be positive"};
    }

    const int n = history.size() / dim;

    struct Match {
        int index;
        double distance;
    };

    std::vector<Match> matches;
    matches.reserve(n);

    for (int i = 0; i < n; ++i) {
        double sum_sq = 0.0;
        const int base = i * dim;
        for (int d = 0; d < dim; ++d) {
            const double diff = history[base + d] - current[d];
            sum_sq += diff * diff;
        }
        matches.push_back({i, std::sqrt(sum_sq)});
    }

    const int take = std::min(k, static_cast<int>(matches.size()));
    std::partial_sort(matches.begin(), matches.begin() + take, matches.end(),
                      [](const Match& a, const Match& b) {
                          return a.distance < b.distance;
                      });

    auto* out_matches = response->mutable_matches();
    out_matches->Reserve(take);
    for (int j = 0; j < take; ++j) {
        auto* m = out_matches->Add();
        m->set_index(matches[j].index);
        m->set_distance(matches[j].distance);
    }

    return grpc::Status::OK;
}

int main() {
    const std::string address = "0.0.0.0:50051";
    SmaServiceImpl service;

    grpc::ServerBuilder builder;
    builder.AddListeningPort(address, grpc::InsecureServerCredentials());
    builder.RegisterService(&service);

    std::unique_ptr<grpc::Server> server(builder.BuildAndStart());
    if (!server) {
        std::cerr << "Failed to start gRPC server on " << address << std::endl;
        return 1;
    }

    std::cout << "Engine listening on " << address << std::endl;
    server->Wait();
    return 0;
}
