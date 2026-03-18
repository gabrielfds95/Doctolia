
-- Table Médecin
/*CREATE TABLE Doctor (
                         id INT PRIMARY KEY AUTO_INCREMENT,
                         first_name VARCHAR(100) NOT NULL,
                         last_name VARCHAR(100) NOT NULL,
                         speciality VARCHAR(100) NOT NULL
);

-- Table Patient
CREATE TABLE Patient (
                         id INT PRIMARY KEY AUTO_INCREMENT,
                         first_name VARCHAR(100) NOT NULL,
                         last_name VARCHAR(100) NOT NULL,
                         age INT NOT NULL
);

-- Table Créneau
CREATE TABLE Slot (
                         id INT PRIMARY KEY AUTO_INCREMENT,
                         slot_date DATE NOT NULL,
                         slot_time TIME NOT NULL,
                         doctor_id INT NOT NULL,
                         patient_id INT NOT NULL,
                         FOREIGN KEY (doctor_id) REFERENCES Doctor(id),
                         FOREIGN KEY (patient_id) REFERENCES Patient(id)
);
*/
INSERT INTO doctor (first_name, last_name, speciality) VALUES
                                                           ('John', 'Dupont', 'Ostheo'),
                                                           ('Paul', 'Dupard', 'Dentiste'),
                                                           ('Jules', 'Pires', 'Generaliste'),
                                                           ('Manuel', 'Meche', 'Generaliste'),
                                                           ('Yves', 'Laure', 'Pediatre');
INSERT INTO patient (first_name, last_name, age) VALUES
                                                     ('Marc', 'Galar', 34),
                                                     ('Jean', 'Dude', 21);

INSERT INTO slot (slot_date, slot_time,end_time,slot_reason, doctor_id, patient_id) VALUES
                                            ('2025-10-01','09:30','10:00','Carries' ,2, 1),
                                            ('2025-10-03','14:30','15:00','Rhume' ,3, 2),
                                            ('2025-10-02','14:30','15:00','Nettoyage' ,2, 1),
                                            ('2025-10-01','16:30','17:00','Remplacement appareil' ,2, 1);