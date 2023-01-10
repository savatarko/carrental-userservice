package org.komponente.userservice.mapper;

import org.komponente.dto.client.ClientCreateDto;
import org.komponente.dto.client.ClientDto;
import org.komponente.userservice.domain.Client;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    public static ClientDto clientToClientDto(Client client)
    {
        ClientDto clientDto = new ClientDto();
        clientDto.setId(client.getId());
        clientDto.setName(client.getName());
        clientDto.setSurname(client.getSurname());
        clientDto.setEmail(client.getEmail());
        clientDto.setNumber(client.getNumber());
        clientDto.setDateofbirth(client.getDateofbirth());
        clientDto.setDaysrented(client.getDaysrented());
        clientDto.setUsername(client.getUsername());
        return clientDto;
    }

    public static Client clientCreateDtoToClient(ClientCreateDto clientCreateDto)
    {
        Client client = new Client();
        client.setName(clientCreateDto.getName());
        client.setSurname(clientCreateDto.getSurname());
        client.setNumber(clientCreateDto.getNumber());
        client.setDateofbirth(clientCreateDto.getDateofbirth());
        client.setPassport(clientCreateDto.getPassport());
        client.setEmail(clientCreateDto.getLoginInfo().getEmail());
        client.setPassword(clientCreateDto.getLoginInfo().getPassword());
        client.setUsername(clientCreateDto.getUsername());
        client.setHasaccess(true);
        client.setTempPassword("");
        client.setHasaccess(false);
        return client;
    }
}
