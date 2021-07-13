package br.com.zup.edu

import br.com.zup.edu.carros.Carro
import br.com.zup.edu.carros.CarroRepository
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

//testes de integração
//o controle transacional é desligado porque os testes rodam em threads separadas
@MicronautTest(transactional = false)//permite levantar todo o contexto de teste do micronatu
//é como se subisse a aplicação para ele mesmo poder testá-la
internal class CarrosEndpointTest(
    private val carroRepository: CarroRepository,
    private val grpcClient: CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub
) {
    // cenario
    @BeforeEach
    internal fun setUp() {
        carroRepository.deleteAll()
    }

    @Test
    fun `deve adicionar um novo carro`() {

        //ação
        val response = grpcClient.adicionar(
            CarrosRequest.newBuilder()
                .setModelo("Gol")
                .setPlaca("AAA-1231")
                .build()
        )
        //validação
        with(response) {
            assertNotNull(id)
            assertTrue(carroRepository.existsById(id)) // efeito colateral - se insiro algo no banco testo se ele tá lá
            //se deleto algo testo se ele nao ta lá
        }
    }

    @Test
    fun `nao deve adicionar novo carro quando placa informada ja existe`() {
        // cenario
        val placa = "ABC-3375"
        carroRepository.save(Carro(modelo = "Astra", placa = placa))
        //acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(
                CarrosRequest.newBuilder()
                    .setModelo("Gol")
                    .setPlaca(placa)
                    .build()
            )
        }
        //validacao
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals(
                "Já existe um veículo com essa placa.",
                status.description
            )//valida se realmente foi essa exceção que foi lançada
        }
    }

    @Test
    fun `nao deve adicionar novo carro quando dados de entrada forem invalidos`() {
        //acao
        val error = assertThrows<StatusRuntimeException> {//assertThrows é para capturar o erro
            grpcClient.adicionar(
                CarrosRequest.newBuilder()
                    .setModelo("")
                    .setPlaca("")
                    .build()
            )
        }
        //validação
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada inválidos.", status.description)
        }
    }

    //classe cliente que atua como se fosse a gente consultando via bloom
    //em GrpcServerChannel.NAME ele vai pegar o nome do server grpc que o micronaut subiu ao invés da porta
    //porque o micronaut sobe os servers de testes em portas aleatórias para caso vc tenha muitos testes
    //vc consiga paralelizar eles
    //Essa classe CarrosGrpcServiceGrpc é injetada lá no construtor da classe
    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub? {
            return CarrosGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}