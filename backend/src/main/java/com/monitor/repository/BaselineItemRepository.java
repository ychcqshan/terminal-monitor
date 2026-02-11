package com.monitor.repository;

import com.monitor.entity.BaselineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaselineItemRepository extends JpaRepository<BaselineItem, Long> {

    List<BaselineItem> findBySnapshotId(Long snapshotId);

    List<BaselineItem> findBySnapshotIdAndItemKey(Long snapshotId, String itemKey);

    void deleteBySnapshotId(Long snapshotId);
}
