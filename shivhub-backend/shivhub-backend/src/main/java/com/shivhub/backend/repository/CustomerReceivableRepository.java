package com.shivhub.backend.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.CustomerReceivable;
import com.shivhub.backend.entity.User;
public interface CustomerReceivableRepository extends JpaRepository<CustomerReceivable, Long> { List<CustomerReceivable> findBySellerOrderByDueDateAsc(User seller); }
