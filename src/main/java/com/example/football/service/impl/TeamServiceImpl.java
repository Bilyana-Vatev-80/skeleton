package com.example.football.service.impl;

import com.example.football.models.dto.TeamSeedDto;
import com.example.football.models.entity.Team;
import com.example.football.repository.TeamRepository;
import com.example.football.service.TeamService;
import com.example.football.service.TownService;
import com.example.football.util.ValidationUtil;
import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Service
public class TeamServiceImpl implements TeamService {
    private static final String TEAM_FILE_PATH = "src/main/resources/files/json/teams.json";
    private final TeamRepository teamRepository;
    private final TownService townService;
    private final Gson gson;
    private final ValidationUtil validationUtil;
    private final ModelMapper modelMapper;

    public TeamServiceImpl(TeamRepository teamRepository, TownService townService, Gson gson, ValidationUtil validationUtil, ModelMapper modelMapper) {
        this.teamRepository = teamRepository;
        this.townService = townService;
        this.gson = gson;
        this.validationUtil = validationUtil;
        this.modelMapper = modelMapper;
    }

    @Override
    public boolean areImported() {
        return this.teamRepository.count() > 0;
    }

    @Override
    public String readTeamsFileContent() throws IOException {
        return Files.readString(Path.of(TEAM_FILE_PATH));
    }

    @Override
    public String importTeams() throws IOException {
        StringBuilder resultInfo = new StringBuilder();

        TeamSeedDto[] teamSeedDtos = gson.fromJson(readTeamsFileContent(), TeamSeedDto[].class);

        Arrays.stream(teamSeedDtos).filter(teamSeedDto -> {
            boolean isValid = validationUtil.isValid(teamSeedDto)
                    && !existsTeam(teamSeedDto.getName())
                    && townService.existsTown(teamSeedDto.getTownName());

            resultInfo
                    .append(isValid ? String.format("Successfully imported Team %s - %d", teamSeedDto.getName(),teamSeedDto.getFanBase())
                            : "Invalid Team")
                    .append(System.lineSeparator());
            return isValid;
        })
                .map(teamSeedDto -> {
                    Team team = modelMapper.map(teamSeedDto, Team.class);
                    team.setTown(townService.findTownByName(teamSeedDto.getTownName()));

                    return team;
                })
                .forEach(this.teamRepository::save);
        return resultInfo.toString();
    }

    @Override
    public boolean existsTeam(String name) {
        return this.teamRepository.existsTeamByName(name);
    }

    @Override
    public Team findTeamByName(String name) {
        return this.teamRepository.findTeamByName(name);
    }
}
