package com.example.football.repository;

import com.example.football.models.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player,Long> {

    boolean existsPlayerByEmail(String email);

    @Query("select p from Player p  join FETCH p.stat s " +
            "where p.birthDate > '1995-01-01' and p.birthDate < '2003-01-01' " +
            "order by s.shooting desc , s.passing desc , s.endurance desc ,p.lastName")
    List<Player> findBestPlayersAndTheirStats();


}
