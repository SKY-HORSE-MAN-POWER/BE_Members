package com.leeforgiveness.memberservice.subscribe.infrastructure;

import com.leeforgiveness.memberservice.subscribe.domain.AuctionSubscription;
import com.leeforgiveness.memberservice.subscribe.state.SubscribeState;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionSubscriptionRepository extends JpaRepository<AuctionSubscription, Long> {

    Optional<AuctionSubscription> findBySubscriberUuidAndAuctionUuid(String subscriberUuid, String auctionUuid);

    Page<AuctionSubscription> findBySubscriberUuidAndState(String subscriberUuid, SubscribeState subscribeState, Pageable pageable);
}