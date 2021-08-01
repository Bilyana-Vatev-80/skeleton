package com.example.football.service.impl;

import com.example.football.models.dto.PlayerSeedRootDto;
import com.example.football.models.entity.Player;
import com.example.football.repository.PlayerRepository;
import com.example.football.service.PlayerService;
import com.example.football.service.StatService;
import com.example.football.service.TeamService;
import com.example.football.service.TownService;
import com.example.football.util.ValidationUtil;
import com.example.football.util.XmlParser;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class PlayerServiceImpl implements PlayerService {
    private static final String PLAYER_FILE_PATH = "src/main/resources/files/xml/players.xml";
    private final XmlParser xmlParser;
    private final ValidationUtil validationUtil;
    private final ModelMapper modelMapper;
    private final PlayerRepository playerRepository;
    private final TownService townService;
    private final TeamService teamService;
    private final StatService statService;

    public PlayerServiceImpl(XmlParser xmlParser, ValidationUtil validationUtil, ModelMapper modelMapper, PlayerRepository playerRepository, TownService townService, TeamService teamService, StatService statService) {
        this.xmlParser = xmlParser;
        this.validationUtil = validationUtil;
        this.modelMapper = modelMapper;
        this.playerRepository = playerRepository;
        this.townService = townService;
        this.teamService = teamService;
        this.statService = statService;
    }

    @Override
    public boolean areImported() {
        return this.playerRepository.count() > 0;
    }

    @Override
    public String readPlayersFileContent() throws IOException {
        return Files.readString(Path.of(PLAYER_FILE_PATH));
    }

    @Override
    public String importPlayers() throws JAXBException, FileNotFoundException {
        StringBuilder resultInfo = new StringBuilder();

        xmlParser.fromFile(PLAYER_FILE_PATH, PlayerSeedRootDto.class)
                .getPlayers()
                .stream()
                .filter(playerSeedDto -> {
                    boolean isValid = validationUtil.isValid(playerSeedDto)
                            && !existsPlayer(playerSeedDto.getEmail())
                            && townService.existsTown(playerSeedDto.getTown().getName())
                            && teamService.existsTeam(playerSeedDto.getTeam().getName());

                    resultInfo
                            .append(isValid ? String.format("Successfully imported Player %s %s - %s", playerSeedDto.getFirstName(),playerSeedDto.getLastName(),playerSeedDto.getPosition())
                                    : "Invalid Player")
                            .append(System.lineSeparator());
                    return isValid;
                })
                .map(playerSeedDto -> {
                    Player player = modelMapper.map(playerSeedDto, Player.class);
                    player.setTeam(teamService.findTeamByName(playerSeedDto.getTeam().getName()));
                    player.setTown(townService.findTownByName(playerSeedDto.getTown().getName()));
                    player.setStat(statService.findStatById(playerSeedDto.getStat().getId()));

                    return player;
                })
                .forEach(this.playerRepository::save);
        return resultInfo.toString();
    }

    @Override
    public String exportBestPlayers() {
        StringBuilder resultInfo = new StringBuilder();

        playerRepository.findBestPlayersAndTheirStats()
                .forEach(player -> {
                    resultInfo
                            .append(String.format("Player - %s %s\n" +
                                    "\tPosition - %s\n" +
                                    "\tTeam - %s\n" +
                                    "\tStadium - %s\n", player.getFirstName(),player.getLastName(),
                                    player.getPosition(),player.getTeam().getName(),player.getTeam().getStadiumName()))
                            .append(System.lineSeparator());
                });
        return resultInfo.toString();
    }

    @Override
    public boolean existsPlayer(String email) {
        return this.playerRepository.existsPlayerByEmail(email);
    }
}
