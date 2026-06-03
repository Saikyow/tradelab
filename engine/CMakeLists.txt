cmake_minimum_required(VERSION 3.16)
project(tradelab_engine CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Находим установленные через Homebrew Protobuf и gRPC
find_package(Protobuf CONFIG REQUIRED)
find_package(gRPC CONFIG REQUIRED)

# Путь к .proto и куда складывать сгенерированный код
set(PROTO_FILE "${CMAKE_SOURCE_DIR}/proto/indicator.proto")
set(GENERATED_DIR "${CMAKE_BINARY_DIR}/generated")
file(MAKE_DIRECTORY ${GENERATED_DIR})

# Имена файлов, которые сгенерит protoc
set(PROTO_SRCS "${GENERATED_DIR}/indicator.pb.cc")
set(PROTO_HDRS "${GENERATED_DIR}/indicator.pb.h")
set(GRPC_SRCS  "${GENERATED_DIR}/indicator.grpc.pb.cc")
set(GRPC_HDRS  "${GENERATED_DIR}/indicator.grpc.pb.h")

# Находим protoc и плагин grpc_cpp_plugin
find_program(PROTOC protoc REQUIRED)
find_program(GRPC_CPP_PLUGIN grpc_cpp_plugin REQUIRED)

# Команда генерации кода из .proto (Protobuf-сообщения + gRPC-сервис)
add_custom_command(
        OUTPUT ${PROTO_SRCS} ${PROTO_HDRS} ${GRPC_SRCS} ${GRPC_HDRS}
        COMMAND ${PROTOC}
        ARGS --proto_path=${CMAKE_SOURCE_DIR}/proto
        --cpp_out=${GENERATED_DIR}
        --grpc_out=${GENERATED_DIR}
        --plugin=protoc-gen-grpc=${GRPC_CPP_PLUGIN}
        ${PROTO_FILE}
        DEPENDS ${PROTO_FILE}
)

# Собираем исполняемый файл сервера
add_executable(server
        src/server.cpp
        ${PROTO_SRCS}
        ${GRPC_SRCS}
)

# Сгенерированные заголовки лежат в GENERATED_DIR — добавим в пути поиска
target_include_directories(server PRIVATE ${GENERATED_DIR})

# Линкуем gRPC и Protobuf
target_link_libraries(server
        gRPC::grpc++
        protobuf::libprotobuf
)