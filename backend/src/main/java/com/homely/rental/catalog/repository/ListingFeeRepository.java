package com.homely.rental.catalog.repository;

import com.homely.rental.catalog.entity.ListingFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingFeeRepository extends JpaRepository<ListingFee, Long> {
    List<ListingFee> findByListingIdOrderBySortOrder(Long listingId);
    void deleteByListingId(Long listingId);
}
