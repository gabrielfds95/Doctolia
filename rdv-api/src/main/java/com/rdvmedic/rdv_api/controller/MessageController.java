package com.rdvmedic.rdv_api.controller;

import com.rdvmedic.rdv_api.dto.MessageCreateDTO;
import com.rdvmedic.rdv_api.dto.MessageDTO;
import com.rdvmedic.rdv_api.security.UserPrincipal;
import com.rdvmedic.rdv_api.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Chat lié à un RDV. Protégé par : SecurityConfig → authenticated() (les deux rôles,
 * PATIENT et DOCTOR, peuvent y accéder — c'est MessageService qui tranche lequel
 * a le droit, via l'ownership, pas SecurityConfig).
 */
@RestController
public class MessageController {

    @Autowired
    private MessageService messageService;

    /** GET /slots/{id}/messages → historique du chat, dans l'ordre chronologique. */
    @GetMapping("/slots/{id}/messages")
    public ResponseEntity<List<MessageDTO>> getMessages(@PathVariable Long id) {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(messageService.getMessages(id, currentUser.getId())
                .stream().map(MessageDTO::fromEntity).toList());
    }

    /** POST /slots/{id}/messages → envoie un message sur ce RDV. */
    @PostMapping("/slots/{id}/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody MessageCreateDTO dto) {
        UserPrincipal currentUser = extractCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var created = messageService.sendMessage(id, currentUser.getId(), dto.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageDTO.fromEntity(created));
    }

    private UserPrincipal extractCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) return null;
        return principal;
    }
}
