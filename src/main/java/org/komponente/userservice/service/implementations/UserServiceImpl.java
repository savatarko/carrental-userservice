package org.komponente.userservice.service.implementations;

import io.github.resilience4j.retry.Retry;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
//import jakarta.transaction.Transactional;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.komponente.dto.client.ClientCreateDto;
import org.komponente.dto.client.ClientDto;
import org.komponente.dto.email.ChangePasswordNotification;
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
import org.komponente.userservice.service.NormalTokenService;
import org.komponente.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jms.*;
import javax.transaction.Transactional;
import javax.validation.constraints.Email;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.jpa.domain.Specification.where;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private AdminRepository adminRepository;
    private ClientRepository clientRepository;
    private ManagerRepository managerRepository;

    @Autowired
    private NormalTokenService tokenService;
    private RankRepository rankRepository;
    private Retry rentalServiceRetry;

    public UserServiceImpl(AdminRepository adminRepository, ClientRepository clientRepository, ManagerRepository managerRepository, NormalTokenService tokenService, RankRepository rankRepository, Retry rentalServiceRetry) {
        this.adminRepository = adminRepository;
        this.clientRepository = clientRepository;
        this.managerRepository = managerRepository;
        this.tokenService = tokenService;
        this.rankRepository = rankRepository;
        this.rentalServiceRetry = rentalServiceRetry;
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
        registerNotification.setLink("http://localhost:8762/users/user/register/confirm/" + activation);
        registerNotification.setReceiverId(manager.getId());

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
        registerNotification.setLink("http://localhost:8762/users/user/register/confirm/" + activation);
        registerNotification.setReceiverId(client.getId());
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

        if(!Objects.equals(changeUserDto.getUsername(), admin.getUsername())) {
            admin.setUsername(changeUserDto.getUsername());
        }
        if(!Objects.equals(changeUserDto.getName(), admin.getName())) {
            admin.setUsername(changeUserDto.getName());
        }
        if(!Objects.equals(changeUserDto.getSurname(), admin.getSurname())) {
            admin.setSurname(changeUserDto.getSurname());
        }
        if(!Objects.equals(changeUserDto.getNumber(), admin.getNumber())){
            admin.setNumber(changeUserDto.getNumber());
        }
        if(!changeUserDto.getDateofbirth().equals(admin.getDateofbirth())){
            admin.setDateofbirth(changeUserDto.getDateofbirth());
        }
        if(!Objects.equals(changeUserDto.getEmail(), admin.getEmail())){
            admin.setDateofbirth(changeUserDto.getDateofbirth());
        }
        String pass = PasswordSecurity.toHexString(PasswordSecurity.getSHA(changeUserDto.getPassword()));
        if(!Objects.equals(changeUserDto.getPassword(), pass)){
            admin.setTempPassword(pass);
            Random random = new Random();
            Long activation = random.nextLong() & Long.MAX_VALUE;
            while(activation!=0 && clientRepository.findClientByConfirmTempPassword(activation).isPresent()
                    && managerRepository.findManagerByConfirmTempPassword(activation).isPresent()
                    && adminRepository.findAdminByConfirmTempPassword(activation).isPresent()) {
                activation = random.nextLong() & Long.MAX_VALUE;
            }
            admin.setConfirmTempPassword(activation);
            admin.setTempPassword(pass);
            ChangePasswordNotification changePasswordNotification = new ChangePasswordNotification();
            changePasswordNotification.setEmail(admin.getEmail());
            changePasswordNotification.setUsername(admin.getUsername());
            changePasswordNotification.setReceiverId(admin.getId());
            changePasswordNotification.setLink("http://localhost:8762/users/user/confirmpass/" + activation);
        }
        adminRepository.save(admin);
    }

    @Override
    public void changeManager(Long managerId, ChangeUserDto changeUserDto) {
        Manager manager = managerRepository.findById(managerId).orElseThrow(()->new NotFoundException("Admin with id " + managerId +" not found!"));

        if(!Objects.equals(changeUserDto.getUsername(), manager.getUsername())) {
            manager.setUsername(changeUserDto.getUsername());
        }
        if(!Objects.equals(changeUserDto.getName(), manager.getName())) {
            manager.setUsername(changeUserDto.getName());
        }
        if(!Objects.equals(changeUserDto.getSurname(), manager.getSurname())) {
            manager.setSurname(changeUserDto.getSurname());
        }
        if(!Objects.equals(changeUserDto.getNumber(), manager.getNumber())){
            manager.setNumber(changeUserDto.getNumber());
        }
        if(!changeUserDto.getDateofbirth().equals(manager.getDateofbirth())){
            manager.setDateofbirth(changeUserDto.getDateofbirth());
        }
        if(changeUserDto.getEmail()!=manager.getEmail()){
            manager.setDateofbirth(changeUserDto.getDateofbirth());
        }
        String pass = PasswordSecurity.toHexString(PasswordSecurity.getSHA(changeUserDto.getPassword()));
        if(!Objects.equals(changeUserDto.getPassword(), pass)){
            manager.setTempPassword(pass);
            Random random = new Random();
            Long activation = random.nextLong() & Long.MAX_VALUE;
            while(activation!=0 && clientRepository.findClientByConfirmTempPassword(activation).isPresent()
                    && managerRepository.findManagerByConfirmTempPassword(activation).isPresent()
                    && adminRepository.findAdminByConfirmTempPassword(activation).isPresent()) {
                activation = random.nextLong() & Long.MAX_VALUE;
            }
            manager.setConfirmTempPassword(activation);
            manager.setTempPassword(pass);
            ChangePasswordNotification changePasswordNotification = new ChangePasswordNotification();
            changePasswordNotification.setEmail(manager.getEmail());
            changePasswordNotification.setUsername(manager.getUsername());
            changePasswordNotification.setReceiverId(manager.getId());
            changePasswordNotification.setLink("http://localhost:8762/users/user/confirmpass/" + activation);
        }
        managerRepository.save(manager);
    }

    @Override
    public void changeClient(Long clientId, ChangeUserDto changeUserDto) {
        Client client = clientRepository.findById(clientId).orElseThrow(()->new NotFoundException("Admin with id " + clientId +" not found!"));

        if(!Objects.equals(changeUserDto.getUsername(), client.getUsername())) {
            client.setUsername(changeUserDto.getUsername());
        }
        if(!Objects.equals(changeUserDto.getName(), client.getUsername())) {
            client.setUsername(changeUserDto.getName());
        }
        if(!Objects.equals(changeUserDto.getSurname(), client.getSurname())) {
            client.setSurname(changeUserDto.getSurname());
        }
        if(!Objects.equals(changeUserDto.getNumber(), client.getNumber())){
            client.setNumber(changeUserDto.getNumber());
        }
        if(!changeUserDto.getDateofbirth().equals(client.getDateofbirth())){
            client.setDateofbirth(changeUserDto.getDateofbirth());
        }
        if(!Objects.equals(changeUserDto.getEmail(), client.getEmail())){
            client.setDateofbirth(changeUserDto.getDateofbirth());
        }
        String pass = PasswordSecurity.toHexString(PasswordSecurity.getSHA(changeUserDto.getPassword()));
        if(!Objects.equals(changeUserDto.getPassword(), pass)){
            Random random = new Random();
            Long activation = random.nextLong() & Long.MAX_VALUE;
            while(activation!=0 && clientRepository.findClientByConfirmTempPassword(activation).isPresent()
                    && managerRepository.findManagerByConfirmTempPassword(activation).isPresent()
                    && adminRepository.findAdminByConfirmTempPassword(activation).isPresent()) {
                activation = random.nextLong() & Long.MAX_VALUE;
            }
            client.setConfirmTempPassword(activation);
            client.setTempPassword(pass);
            ChangePasswordNotification changePasswordNotification = new ChangePasswordNotification();
            changePasswordNotification.setEmail(client.getEmail());
            changePasswordNotification.setUsername(client.getUsername());
            changePasswordNotification.setReceiverId(client.getId());
            changePasswordNotification.setLink("http://localhost:8762/users/user/confirmpass/" + activation);
        }
        if(!Objects.equals(changeUserDto.getPassport(), client.getPassport())){
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

    @Override
    public String findClientMail(Long id) {
        Admin admin = adminRepository.findById(id).orElse(null);
        adminRepository.findAll();
        if(admin!=null)
        {
            return admin.getEmail();
        }
        Manager manager = managerRepository.findById(id).orElse(null);
        if (manager!=null)
        {
            return manager.getEmail();
        }
        Client client = clientRepository.findById(id).orElse(null);
        if(client!=null)
        {
            return client.getEmail();
        }
        throw new NotFoundException("User with id " + id + " not found!");
    }

    @Override
    public List<UserDto> getAllUsers() {
        List<UserDto> output = new ArrayList<>();
        output.addAll(clientRepository.findAll().stream().map(UserMapper::userToUserDto).collect(Collectors.toList()));
        output.addAll(managerRepository.findAll().stream().map(UserMapper::userToUserDto).collect(Collectors.toList()));
        return output;
    }

    @Override
    public List<RankDto> getAllRanks() {
        return rankRepository.findAll().stream().map(RankMapper::rankToRankDto).collect(Collectors.toList());
    }

    @Override
    public void confirmPassword(Long id) {
        if(id == 0L){
            throw new ZeroIndexException("Zero index!");
        }
        Admin admin = adminRepository.findAdminByConfirmTempPassword(id).orElse(null);
        if(admin!=null)
        {
            admin.setPassword(admin.getTempPassword());
            admin.setConfirmTempPassword(0L);
            admin.setTempPassword("");
            adminRepository.save(admin);
            return;
        }
        Manager manager = managerRepository.findManagerByConfirmTempPassword(id).orElse(null);
        if(manager!=null)
        {
            manager.setPassword(manager.getTempPassword());
            manager.setConfirmTempPassword(0L);
            manager.setTempPassword("");
            managerRepository.save(manager);
            return;
        }
        Client client = clientRepository.findClientByConfirmTempPassword(id).orElseThrow(()->new NotFoundException("User with confirmation " + id +" not found!"));
        client.setPassword(client.getTempPassword());
        client.setConfirmTempPassword(0L);
        client.setTempPassword("");
        clientRepository.save(client);
    }


}
