package com.rdvmedic.rdv_api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "admins")
@Data
@NoArgsConstructor
@SuperBuilder
@DiscriminatorValue("ADMIN")
public class Admin extends User {
    // Aucun champ supplémentaire — le super admin hérite des champs User
}
