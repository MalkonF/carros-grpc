package br.com.zup.edu.carros

import br.com.zup.edu.CarrosGrpcServiceGrpc
import br.com.zup.edu.CarrosRequest
import br.com.zup.edu.CarrosResponse
import io.grpc.Status
import io.grpc.stub.StreamObserver
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
class CarrosEndpoint(val carroRepository: CarroRepository) : CarrosGrpcServiceGrpc.CarrosGrpcServiceImplBase() {
    override fun adicionar(request: CarrosRequest, responseObserver: StreamObserver<CarrosResponse>) {
        if (carroRepository.existsByPlaca(request.placa)) {
            responseObserver.onError(
                Status.ALREADY_EXISTS
                    .withDescription("Já existe um veículo com essa placa.")
                    .asRuntimeException()
            )
            return
        }

        val novoCarro = Carro(placa = request.placa, modelo = request.modelo)
        try {
            carroRepository.save(novoCarro)
        } catch (e: ConstraintViolationException) {// se der algum erro de validação tipo notBlank etc
            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("dados de entrada inválidos.")
                    .asRuntimeException()
            )
            return
        }

        responseObserver.onNext(CarrosResponse.newBuilder().setId(novoCarro.id!!).build())
        responseObserver.onCompleted()
    }
}