package org.komponente.userservice.service.implementations;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
//import jakarta.transaction.Transactional;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.komponente.dto.client.ClientCreateDto;
import org.komponente.dto.client.ClientDto;
import org.komponente.dto.email.RegisterNotification;
import org.komponente.dto.manager.ManagerCreateDto;
import org.komponente.dto.manager.ManagerDto;
import org.komponente.dto.rank.RankCreateDto;
import org.komponente.dto.rank.RankDto;
import org.komponente.dto.token.TokenRequestDto;
import org.komponente.dto.token.TokenResponseDto;
import org.komponente.dto.user.ChangeUserDto;
import org.komponente.dto.user.UserDto;
import org.komponente.userservice.domain.*;
import org.komponente.userservice.exceptions.*;
import org.komponente.userservice.mapper.ClientMapper;
import org.komponente.userservice.mapper.ManagerMapper;
import org.komponente.userservice.mapper.RankMapper;
import org.komponente.userservice.mapper.UserMapper;
import org.komponente.userservice.repository.AdminRepository;
import org.komponente.userservice.repository.ClientRepository;
import org.komponente.userservice.repository.ManagerRepository;
import org.komponente.userservice.repository.RankRepository;
import org.komponente.userservice.security.PasswordSecurity;
import org.komponente.userservice.security.token.TokenService;
import org.komponente.userservice.service.UserService;
import org.springframework.stereotype.Service;

import javax.jms.*;
import javax.transaction.Transactional;
import javax.validation.constraints.Email;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private AdminRepository adminRepository;
    private ClientRepository clientRepository;
    private ManagerRepository managerRepository;
    private TokenService tokenService;
    private RankRepository rankRepository;

    public UserServiceImpl(AdminRepository adminRepository, ClientRepository clientRepository, ManagerRepository managerRepository, TokenService tokenService, RankRepository rankRepository) {
        this.adminRepository = adminRepository;
        this.clientRepository = clientRepository;
        this.managerRepository = managerRepository;
        this.tokenService = tokenService;
        this.rankRepository = rankRepository;
    }

    private void sendMessage(Serializable content, String queueName) {
        try {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
            Connection connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = new ActiveMQQueue(queueName);
            MessageProducer producer = session.createProducer(destination);
            ObjectMessage message = session.createObjectMessage(content);
            producer.send(message);
            producer.close();
            session.close();
            connection.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public TokenResponseDto login(TokenRequestDto tokenRequestDto) {
        Claims claims = Jwts.claims();
        String pass = PasswordSecurity.toHexString(PasswordSecurity.getSHA(tokenRequestDto.getPassword()));
        Admin admin = adminRepository.findAdminByUsernameAndPassword(tokenRequestDto.getUsername(), pass).orElse(null);
        if(admin != null)
        {
            claims.put("id", admin.getId());
            claims.put("role", "ROLE_ADMIN");
            return new TokenResponseDto(tokenService.generate(claims));
        }
        Manager manager = managerRepository.findManagerByUsernameAndPassword(tokenRequestDto.getUsername(), pass).orElse(null);
        //System.out.println("in 4");
        if(manager!= null)
        {
            if(manager.getHasaccess() == false)
            {
                throw new AccessRevokedException("Access revoked! Contact a admin to get access back.");
            }
            if(manager.getActivated() !=0){
                throw new NotActivatedException("Account not activated! Check your email for activation link.");
            }
            claims.put("id", manager.getId());
            claims.put("role", "ROLE_MANAGER");
            return new TokenResponseDto(tokenService.generate(claims));
        }
        Client client = clientRepository.findClientByUsernameAndPassword(tokenRequestDto.getUsername(), pass)
                .orElseThrow(() -> new NotFoundException(String
                        .format("User with username: %s and password: %s not found.", tokenRequestDto.getUsername(),
                                tokenRequestDto.getPassword())));
        if(client.getHasaccess() == false)
        {
            throw new AccessRevokedException("Access revoked! Contact a admin to get access back.");
        }
        if(client.getActivated() != 0){
            throw new NotActivatedException("Account not activated! Check your email for activation link.");
        }
        claims.put("id", client.getId());
        claims.put("role", "ROLE_CLIENT");
        return new TokenResponseDto(tokenService.generate(claims));
    }

    @Override
    public ManagerDto add(ManagerCreateDto managerCreateDto) {
        if(managerRepository.findManagerByUsername(managerCreateDto.getUsername()).isPresent())
        {
            throw new UsernameAlreadyInUseException("A user with this username already exists!");
        }
        if(managerRepository.findManagerByUsername(managerCreateDto.getEmail()).isPresent())
        {
            throw new EmailAlreadyExistException("A user with this email already exists!");
        }
        Manager manager = ManagerMapper.managerCreateDtoToManager(managerCreateDto);

        Random random = new Random();
        Long activation = random.nextLong() & Long.MAX_VALUE;
        while(activation!=0 && clientRepository.findClientByActivated(activation).isPresent()
                && managerRepository.findManagerByActivated(activation).isPresent()) {
            activation = random.nextLong() & Long.MAX_VALUE;
        }

        manager.setActivated(activation);
        managerRepository.save(manager);
        RegisterNotification registerNotification = new RegisterNotification();
        registerNotification.setEmail(manager.getEmail());
        registerNotification.setName(manager.getName());
        registerNotification.setSurname(manager.getSurname());
        registerNotification.setLink("http://localhost:8081/user/register/confirm/" + activation);

        sendMessage(registerNotification, "newregistration");
        return ManagerMapper.managerToManagerDto(manager);
    }

    @Override
    public ClientDto add(ClientCreateDto clientCreateDto) {
        if(clientRepository.findClientByUsername(clientCreateDto.getUsername()).isPresent())
        {
            throw new UsernameAlreadyInUseException("A user with this username already exists!");
        }
        if(clientRepository.findClientByEmail(clientCreateDto.getEmail()).isPresent())
        {
            throw new EmailAlreadyExistException("A user with this email already exists!");
        }
        Client client = ClientMapper.clientCreateDtoToClient(clientCreateDto);
        Random random = new Random();
        Long activation = random.nextLong() & Long.MAX_VALUE;
        while(activation!=0 && clientRepository.findClientByActivated(activation).isPresent()
                && managerRepository.findManagerByActivated(activation).isPresent()) {
            activation = random.nextLong() & Long.MAX_VALUE;
        }
        client.setActivated(activation);

        clientRepository.save(client);
        RegisterNotification registerNotification = new RegisterNotification();
        registerNotification.setEmail(client.getEmail());
        registerNotification.setName(client.getName());
        registerNotification.setSurname(client.getSurname());
        registerNotification.setLink("http://localhost:8080/register/confirm/" + activation);
        sendMessage(registerNotification, "newregistration");
        return ClientMapper.clientToClientDto(client);
    }

    @Override
    public void removeAccess(Long id) {
        Admin admin = adminRepository.findById(id).orElse(null);
        if(admin!=null)
        {
            throw new RevokeAdminException("You can't revoke access from a admin!");
        }
        Manager manager = managerRepository.findById(id).orElse(null);
        if(manager!=null)
        {
            if(manager.getHasaccess() == false)
            {
                throw new AlreadyRevokedException("Used with the id " + id + " can't login already!");
            }
            manager.setHasaccess(false);
            managerRepository.save(manager);
            return;
        }
        Client client = clientRepository.findById(id).orElseThrow(() -> new NotFoundException("User with the id " + id + " not found"));
        if(client.getHasaccess() == false)
        {
            throw new AlreadyRevokedException("Used with the id " + id + " can't login already!");
        }
        client.setHasaccess(false);
        clientRepository.save(client);
    }

    //mozda da prosledimo ovde username, email ili dto?
    @Override
    public void giveAccess(Long id) {
        Admin admin = adminRepository.findById(id).orElse(null);
        if(admin!=null)
        {
            throw new RevokeAdminException("You can't revoke access from a admin!");
        }
        Manager manager = managerRepository.findById(id).orElse(null);
        if(manager!=null)
        {
            if(manager.getHasaccess() == true)
            {
                throw new AlreadyAllowedException("Used with the id " + id + " can login already!");
            }
            manager.setHasaccess(true);
            managerRepository.save(manager);
            return;
        }
        Client client = clientRepository.findById(id).orElseThrow(() -> new NotFoundException("User with the id " + id + " not found"));
        if(client.getHasaccess() == true)
        {
            throw new AlreadyAllowedException("Used with the id " + id + " can login already!");
        }
        client.setHasaccess(true);
        clientRepository.save(client);
    }

    @Override
    public RankDto add(RankCreateDto rankCreateDto) {
        Rank check = rankRepository.findRankByName(rankCreateDto.getName()).orElse(null);
        if(check!=null)
        {
            throw new RankAlreadyExistsException("Rank with the name " + rankCreateDto.getName() + " already exists!");
        }
        Rank rank = RankMapper.rankCreateDtoToRank(rankCreateDto);
        rankRepository.save(rank);
        return RankMapper.rankToRankDto(rank);
    }

    @Override
    public RankDto changeRank(Long id, RankCreateDto rankDto) {
        Rank rank = rankRepository.findById(id).orElseThrow(()->new NotFoundException("Rank with id " + id +" not found!"));

        rank.setValue(rankDto.getValue());
        rank.setName(rankDto.getName());
        rank.setNumberofdays(rankDto.getNumberofdays());
        rankRepository.save(rank);
        return RankMapper.rankToRankDto(rank);
    }

    @Override
    public Long getRank(Long userid) {
        Client client = clientRepository.findById(userid).orElseThrow(()->new NotFoundException("User with id " + userid +" not found!"));
        Integer rentdays = client.getDaysrented();
        List<Rank> ranks = rankRepository.findAll();
        ranks.sort(Comparator.comparingInt(Rank::getNumberofdays));
        for(Rank rank: ranks)
        {
            if(rank.getNumberofdays() < client.getDaysrented())
            {
                return rank.getValue();
            }
        }
        return ranks.get(ranks.size() -1 ).getValue();
    }

    @Override
    public void changeAdmin(Long adminId, ChangeUserDto changeUserDto) {
        Admin admin = adminRepository.findById(adminId).orElseThrow(()->new NotFoundException("Admin with id " + adminId +" not found!"));

        if(changeUserDto.getUsername()!=null) {
            admin.setUsername(changeUserDto.getUsername());
        }
        if(changeUserDto.getName()!=null) {
            admin.setUsername(changeUserDto.getName());
        }
        if(changeUserDto.getSurname()!=null) {
            admin.setSurname(changeUserDto.getSurname());
        }
        if(changeUserDto.getNumber()!=null){
            admin.setNumber(changeUserDto.getNumber());
        }
        if(changeUserDto.getDateofbirth()!=null){
            admin.setDateofbirth(changeUserDto.getDateofbirth());
        }
        if(changeUserDto.getEmail()!=null){
            admin.setDateofbirth(changeUserDto.getDateofbirth());
        }
        if(changeUserDto.getPassword()!=null){
            admin.setTempPassword(changeUserDto.getPassword());
        }
        adminRepository.save(admin);
    }

    @Override
    public void changeManager(Long managerId, ChangeUserDto changeUserDto) {
        Manager manager = managerRepository.findById(managerId).orElseThrow(()->new NotFoundException("Admin with id " + managerId +" not found!"));

        if(changeUserDto.getUsername()!=null) {
            manager.setUsername(changeUserDto.getUsername());
        }
        if(changeUserDto.getName()!=null) {
            manager.setUsername(changeUserDto.getName());
        }
        if(changeUserDto.getSurname()!=null) {
            manager.setSurname(changeUserDto.getSurname());
        }
        if(changeUserDto.getNumber()!=null){
            manager.setNumber(changeUserDto.getNumber());
        }
        if(changeUserDto.getDateofbirth()!=null){
            manager.setDateofbirth(changeUserDto.getDateofbirth());
        }
        if(changeUserDto.getEmail()!=null){
            manager.setDateofbirth(changeUserDto.getDateofbirth());
        }
        if(changeUserDto.getPassword()!=null){
            manager.setTempPassword(changeUserDto.getPassword());
        }
        managerRepository.save(manager);
    }

    @Override
    public void changeClient(Long clientId, ChangeUserDto changeUserDto) {
        Client client = clientRepository.findById(clientId).orElseThrow(()->new NotFoundException("Admin with id " + clientId +" not found!"));

        if(changeUserDto.getUsername()!=null) {
            client.setUsername(changeUserDto.getUsername());
        }
        if(changeUserDto.getName()!=null) {
            client.setUsername(changeUserDto.getName());
        }
        if(changeUserDto.getSurname()!=null) {
            client.setSurname(changeUserDto.getSurname());
        }
        if(changeUserDto.getNumber()!=null){
            client.setNumber(changeUserDto.getNumber());
        }
        if(changeUserDto.getDateofbirth()!=null){
            client.setDateofbirth(changeUserDto.getDateofbirth());
        }
        if(changeUserDto.getEmail()!=null){
            client.setDateofbirth(changeUserDto.getDateofbirth());
        }
        if(changeUserDto.getPassword()!=null){
            client.setTempPassword(changeUserDto.getPassword());
        }
        if(changeUserDto.getPassport()!=null){
            client.setPassport(changeUserDto.getPassport());
        }
        clientRepository.save(client);
    }

    @Override
    public void activateAccount(Long confirmation) {
        Manager manager = managerRepository.findManagerByActivated(confirmation).orElse(null);
        if(manager!=null)
        {
            manager.setActivated(0L);
            managerRepository.save(manager);
            return;
        }
        Client client = clientRepository.findClientByActivated(confirmation).orElseThrow(()->new NotFoundException("User with confirmation " + confirmation +" not found!"));
        client.setActivated(0L);
        clientRepository.save(client);
    }

    @Override
    public Long getManagerCompanyId(Long id) {
        Manager manager = managerRepository.findById(id).orElseThrow(()->new NotFoundException("Manager with id " + id + " not found!"));
        return manager.getCompanyid();
    }

    @Override
    public UserDto findUser(Long id) {
        Admin admin = adminRepository.findById(id).orElse(null);
        if(admin!=null)
        {
            return UserMapper.userToUserDto(admin);
        }
        Manager manager = managerRepository.findById(id).orElse(null);
        if (manager!=null)
        {
            return UserMapper.userToUserDto(manager);
        }
        Client client = clientRepository.findById(id).orElse(null);
        if(client!=null)
        {
            return UserMapper.userToUserDto(client);
        }
        throw new NotFoundException("User with id " + id + " not found!");
    }


}
