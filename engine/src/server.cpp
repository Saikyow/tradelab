#include "server.h"

#include <cmath>
#include <iostream>
#include <memory>
#include <string>

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
