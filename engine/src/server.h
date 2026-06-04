#ifndef TRADELAB_ENGINE_SERVER_H
#define TRADELAB_ENGINE_SERVER_H

#include <grpcpp/grpcpp.h>
#include "indicator.grpc.pb.h"

class SmaServiceImpl final : public indicator::IndicatorService::Service {
public:
    grpc::Status ComputeSma(grpc::ServerContext* context,
                            const indicator::SmaRequest* request,
                            indicator::SmaResponse* response) override;

    grpc::Status ComputeEma(grpc::ServerContext* context,
                            const indicator::EmaRequest* request,
                            indicator::EmaResponse* response) override;

    grpc::Status ComputeRsi(grpc::ServerContext* context,
                            const indicator::RsiRequest* request,
                            indicator::RsiResponse* response) override;
};

#endif //TRADELAB_ENGINE_SERVER_H
