package com.urlshortener.urlshortener.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.urlshortener.urlshortener.entity.ClickEvent;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByShortCode(String shortCode);

    @Query("select c.browser, count(c) from ClickEvent c where c.shortCode = :code group by c.browser")
    List<Object[]> countByBrowser(@Param("code") String code);

    @Query("select c.device, count(c) from ClickEvent c where c.shortCode = :code group by c.device")
    List<Object[]> countByDevice(@Param("code") String code);
}