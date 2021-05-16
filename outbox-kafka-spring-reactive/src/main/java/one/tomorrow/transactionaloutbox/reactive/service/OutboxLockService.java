package one.tomorrow.transactionaloutbox.reactive.service;

import lombok.RequiredArgsConstructor;
import one.tomorrow.transactionaloutbox.reactive.repository.OutboxLockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OutboxLockService {

	private static final Logger logger = LoggerFactory.getLogger(OutboxLockService.class);

	private final OutboxLockRepository repository;
	private final TransactionalOperator rxtx;

	public Mono<Boolean> acquireOrRefreshLock(String ownerId, Duration lockTimeout, boolean refreshLock) {
		return repository.acquireOrRefreshLock(ownerId, lockTimeout, refreshLock);
	}

	public Mono<Void> releaseLock(String ownerId) {
		return repository.releaseLock(ownerId);
	}

	@SuppressWarnings("java:S5411")
	public Mono<Boolean> runWithLock(String ownerId, Mono<Void> action) {
		return repository.preventLockStealing(ownerId).flatMap(outboxLockIsPreventedFromLockStealing ->
				outboxLockIsPreventedFromLockStealing
						? action.thenReturn(true)
						: Mono.just(false)
		).as(rxtx::transactional);
	}

}