package com.gsswec.ecommerce.products.infrastructure.grpc;

import com.gsswec.ecommerce.products.application.usecase.ReserveStock;
import com.gsswec.ecommerce.stock.grpc.ReleaseRequest;
import com.gsswec.ecommerce.stock.grpc.ReleaseResponse;
import com.gsswec.ecommerce.stock.grpc.ReserveRequest;
import com.gsswec.ecommerce.stock.grpc.ReserveResponse;
import com.gsswec.ecommerce.stock.grpc.ShortfallReason;
import com.gsswec.ecommerce.stock.grpc.StockServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.util.List;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class StockGrpcService extends StockServiceGrpc.StockServiceImplBase {

    private final ReserveStock reserveStock;

    public StockGrpcService(ReserveStock reserveStock) {
        this.reserveStock = reserveStock;
    }

    @Override
    public void reserve(ReserveRequest request, StreamObserver<ReserveResponse> responseObserver) {
        List<ReserveStock.Line> lines = request.getLinesList().stream()
                .map(l -> new ReserveStock.Line(l.getProductId(), l.getSku(), l.getQuantity()))
                .toList();

        ReserveResponse response;
        try {
            ReserveStock.Result result = reserveStock.reserve(lines);
            ReserveResponse.Builder builder = ReserveResponse.newBuilder().setReserved(true);
            for (ReserveStock.ReservedLine rl : result.lines()) {
                builder.addLines(com.gsswec.ecommerce.stock.grpc.ReservedLine.newBuilder()
                        .setProductId(rl.productId())
                        .setSku(rl.sku())
                        .setName(rl.name())
                        .setUnitPrice(rl.unitPrice().toPlainString())
                        .setQuantity(rl.quantity())
                        .build());
            }
            response = builder.build();
        } catch (ReserveStock.ReservationFailed e) {
            ReserveResponse.Builder builder = ReserveResponse.newBuilder().setReserved(false);
            for (ReserveStock.Shortfall s : e.shortfalls()) {
                builder.addShortfalls(com.gsswec.ecommerce.stock.grpc.StockShortfall.newBuilder()
                        .setProductId(s.line().productId())
                        .setSku(s.line().sku())
                        .setRequested(s.line().quantity())
                        .setAvailable(s.available())
                        .setReason(toProtoReason(s.reason()))
                        .build());
            }
            response = builder.build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void release(ReleaseRequest request, StreamObserver<ReleaseResponse> responseObserver) {
        List<ReserveStock.Line> lines = request.getLinesList().stream()
                .map(l -> new ReserveStock.Line(l.getProductId(), l.getSku(), l.getQuantity()))
                .toList();
        reserveStock.release(lines);
        responseObserver.onNext(ReleaseResponse.newBuilder().setReleased(true).build());
        responseObserver.onCompleted();
    }

    private static ShortfallReason toProtoReason(ReserveStock.Reason reason) {
        return switch (reason) {
            case INSUFFICIENT_STOCK -> ShortfallReason.INSUFFICIENT_STOCK;
            case PRODUCT_NOT_FOUND -> ShortfallReason.PRODUCT_NOT_FOUND;
            case PRODUCT_INACTIVE -> ShortfallReason.PRODUCT_INACTIVE;
        };
    }
}
