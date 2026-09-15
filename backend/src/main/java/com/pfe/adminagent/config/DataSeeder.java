package com.pfe.adminagent.config;

import com.pfe.adminagent.leave.LeaveBalanceService;
import com.pfe.adminagent.user.domain.Role;
import com.pfe.adminagent.user.domain.User;
import com.pfe.adminagent.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a small set of demo users on first startup (dev/docker profiles only),
 * so the dashboard and mobile app have accounts to sign in with immediately.
 * Idempotent: skips creation if the account already exists.
 */
@Component
@Profile({"docker", "dev", "default"})
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEFAULT_PASSWORD = "Password123!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeaveBalanceService leaveBalanceService;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                      LeaveBalanceService leaveBalanceService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.leaveBalanceService = leaveBalanceService;
    }

    // Real TGR central directions — used to scope managers to their own direction.
    private static final String D_RECH = "Direction de la Recherche, de la Réglementation et de la Coopération Internationale";
    private static final String D_FIN  = "Direction des Finances Publiques";
    private static final String D_DEP  = "Direction des Dépenses du Personnel";
    private static final String D_CPT  = "Direction des Comptes Publics";
    private static final String D_SI   = "Direction des Ressources et du Système d'Information";
    private static final String D_CTRL = "Direction du Contrôle, de l'Audit et de l'Inspection";

    @Override
    public void run(String... args) {
        // --- Primary demo direction : Ressources et Système d'Information (direction du stage) ---
        seed("employe@adminai.ma", "Yassine El Amrani", Role.EMPLOYEE, D_SI, "Chargé d'études", 8);
        // Plusieurs responsables peuvent valider les demandes (pas de rôle « directeur »).
        seed("manager@adminai.ma", "Fatima Zahra Bennani", Role.MANAGER, D_SI, "Responsable", 4);
        seed("directeur@adminai.ma", "Rachid Alaoui", Role.MANAGER, D_SI, "Responsable", 11);
        seed("admin@adminai.ma", "Administrateur Système", Role.ADMIN, D_SI, "Administrateur", 2);

        // --- One manager + one employee per remaining central direction (per-direction scoping) ---
        seed("manager.recherche@adminai.ma", "Khadija Berrada", Role.MANAGER, D_RECH, "Chef de division", 5);
        seed("employe.recherche@adminai.ma", "Omar Fassi", Role.EMPLOYEE, D_RECH, "Cadre", 3);

        seed("manager.finances@adminai.ma", "Hamid Tazi", Role.MANAGER, D_FIN, "Chef de division", 6);
        seed("employe.finances@adminai.ma", "Salma Idrissi", Role.EMPLOYEE, D_FIN, "Cadre", 2);

        seed("manager.depenses@adminai.ma", "Nadia Chraibi", Role.MANAGER, D_DEP, "Chef de division", 7);
        seed("employe.depenses@adminai.ma", "Youssef Amrani", Role.EMPLOYEE, D_DEP, "Agent", 4);

        seed("manager.comptes@adminai.ma", "Karim Belhaj", Role.MANAGER, D_CPT, "Chef de division", 3);
        seed("employe.comptes@adminai.ma", "Imane Saidi", Role.EMPLOYEE, D_CPT, "Cadre", 1);

        seed("manager.controle@adminai.ma", "Rachida Ouazzani", Role.MANAGER, D_CTRL, "Chef de division", 9);
        seed("employe.controle@adminai.ma", "Mehdi Alaoui", Role.EMPLOYEE, D_CTRL, "Auditeur", 2);
    }

    private void seed(String email, String fullName, Role role, String department, String jobTitle, double usedLeaveDays) {
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            user = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .fullName(fullName)
                    .role(role)
                    .department(department)
                    .jobTitle(jobTitle)
                    .active(true)
                    .build();
            userRepository.save(user);
            log.info("Seeded demo user [{}] with role {}", email, role);
        } else {
            // Keep the demo accounts aligned with the intended role/direction on each
            // startup (so per-direction scoping is deterministic without wiping the DB).
            user.setFullName(fullName);
            user.setRole(role);
            user.setDepartment(department);
            user.setJobTitle(jobTitle);
            user.setActive(true);
            userRepository.save(user);
        }
        // Ensure a leave balance exists even for pre-existing demo accounts.
        leaveBalanceService.ensureExists(user.getId(), usedLeaveDays);
    }
}
