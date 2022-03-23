package br.com.verbososcorp.ilocation.services.interfaces;

import br.com.verbososcorp.ilocation.models.DeliveryPerson;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface DeliveryPersonService {
    ResponseEntity<DeliveryPerson> register(DeliveryPerson newDeliveryPerson);

    ResponseEntity<DeliveryPerson> getByEmail(String email);

    ResponseEntity<List<DeliveryPerson>> getAll();
}
