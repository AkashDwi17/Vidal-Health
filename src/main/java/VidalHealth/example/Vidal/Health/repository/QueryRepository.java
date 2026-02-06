package VidalHealth.example.Vidal.Health.repository;

import VidalHealth.example.Vidal.Health.entity.FinalQueryEntity;
import org.springframework.stereotype.Repository;

@Repository
public class QueryRepository {

    private FinalQueryEntity storedQuery;

    public void save(FinalQueryEntity entity) {
        this.storedQuery = entity;
    }

    public FinalQueryEntity get() {
        return storedQuery;
    }
}
