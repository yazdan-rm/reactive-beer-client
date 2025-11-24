package ir.nrdc.reactivebeerclient.client;

import ir.nrdc.reactivebeerclient.config.WebClientConfig;
import ir.nrdc.reactivebeerclient.model.BeerDto;
import ir.nrdc.reactivebeerclient.model.BeerPagedList;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
class BeerClientImplTest {

    BeerClient beerClient;

    @BeforeEach
    void setUp() {
        beerClient = new BeerClientImpl(new WebClientConfig().webClient());
    }

    @Test
    void getBeerById() {
        var beerListMono = beerClient.listBeers(null, null, null, null, null);
        BeerPagedList beerPagedList = beerListMono.block();
        Assertions.assertNotNull(beerPagedList);
        BeerDto firstBeer = beerPagedList.getContent().getFirst();

        Mono<BeerDto> getByIdBeerDtoMono = beerClient.getBeerById(firstBeer.getId(), Boolean.TRUE);
        BeerDto getByIdBeerDto = getByIdBeerDtoMono.block();
        assertThat(getByIdBeerDto).isNotNull();
        assertThat(getByIdBeerDto.getId()).isEqualTo(firstBeer.getId());
        System.out.println(getByIdBeerDto);
    }

    @Test
    void listBeers() {
        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(null, null, null,
                null, null);

        BeerPagedList beerPagedList = beerPagedListMono.block();

        assertThat(beerPagedList).isNotNull();
        assertThat(beerPagedList.getContent().size()).isGreaterThan(0);
        System.out.println(beerPagedList.getContent());
    }

    @Test
    void listBeersSize10() {
        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(1, 10, null,
                null, null);

        BeerPagedList beerPagedList = beerPagedListMono.block();

        assertThat(beerPagedList).isNotNull();
        assertThat(beerPagedList.getContent().size()).isEqualTo(10);
        System.out.println(beerPagedList.getContent());
    }

    @Test
    void listBeersNoRecords() {
        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(10, 20, null,
                null, null);

        BeerPagedList beerPagedList = beerPagedListMono.block();

        assertThat(beerPagedList).isNotNull();
        assertThat(beerPagedList.getContent().size()).isEqualTo(0);
        System.out.println(beerPagedList.getContent());
    }

    @Test
    void getBeerByUPC() {
        Mono<BeerPagedList> beerPagedListMono = beerClient.listBeers(null, null, null,
                null, null);

        String beerUpcExpected = Objects.requireNonNull(beerPagedListMono.block()).getContent().getFirst().getUpc();

        Mono<BeerDto> beerDtoMono = beerClient.getBeerByUPC(beerUpcExpected);
        String beerUpcActual = beerDtoMono.block().getUpc();

        assertThat(beerUpcActual).isEqualTo(beerUpcExpected);
    }

    @Test
    void createBeer() {
        BeerDto beerDto = BeerDto.builder()
                .beerName("Doglost 90 Min IPS")
                .beerStyle("IPA")
                .upc("1123345435632")
                .price(new BigDecimal("10.99"))
                .build();

        Mono<ResponseEntity<Void>> responseEntityMono = beerClient.createBeer(beerDto);
        ResponseEntity<Void> responseEntity = responseEntityMono.block();
        Assertions.assertNotNull(responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void updateBeer() {
        var beerListMono = beerClient.listBeers(null, null, null, null, null);
        BeerPagedList beerPagedList = beerListMono.block();
        Assertions.assertNotNull(beerPagedList);
        BeerDto firstBeerDto = beerPagedList.getContent().getFirst();

        BeerDto beerDto = BeerDto.builder()
                .beerName("test yazdan")
                .beerStyle(firstBeerDto.getBeerStyle())
                .price(firstBeerDto.getPrice())
                .upc(firstBeerDto.getUpc())
                .build();

        Mono<ResponseEntity<Void>> responseEntityMono = beerClient.updateBeer(beerDto.getId(), beerDto);
        ResponseEntity<Void> responseEntity = responseEntityMono.block();
        Assertions.assertNotNull(responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteBeerById() {
        var beerListMono = beerClient.listBeers(null, null, null, null, null);
        BeerPagedList beerPagedList = beerListMono.block();
        Assertions.assertNotNull(beerPagedList);
        BeerDto firstBeerDto = beerPagedList.getContent().getFirst();

        Mono<ResponseEntity<Void>> responseEntityMono = beerClient.deleteBeerById(firstBeerDto.getId());
        ResponseEntity<Void> responseEntity = responseEntityMono.block();
        Assertions.assertNotNull(responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void testDeleteBeerHandleException() {
        Mono<ResponseEntity<Void>> responseEntityMono = beerClient.deleteBeerById(UUID.randomUUID());

        ResponseEntity<Void> responseEntity = responseEntityMono.onErrorResume(throwable -> {
            if (throwable instanceof WebClientResponseException webClientResponseException) {
                return Mono.just(ResponseEntity.status(webClientResponseException.getStatusCode()).build());
            } else {
                throw new RuntimeException(throwable);
            }
        }).block();

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteBeerNotFound() {
        Mono<ResponseEntity<Void>> responseEntityMono = beerClient.deleteBeerById(UUID.randomUUID());
        assertThrows(WebClientResponseException.class, () -> {
            ResponseEntity<Void> responseEntity = responseEntityMono.block();
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        });
    }

    @Test
    void functionalTestGetBeerById() throws InterruptedException {
        AtomicReference<String> beerDto = new AtomicReference<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);

        beerClient.listBeers(null, null, null, null, null)
                .map(beerPagedList -> beerPagedList.getContent().getFirst().getId())
                .map(uuid -> beerClient.getBeerById(uuid, false))
                .flatMap(beer -> beer)
                .subscribe(beer -> {
                    countDownLatch.countDown();
                    beerDto.set(beer.getBeerName());
                });

        countDownLatch.await();

        assertThat(beerDto.get()).isEqualTo("TEST");
    }
}