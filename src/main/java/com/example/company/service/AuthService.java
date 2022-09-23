package com.example.company.service;


import com.example.company.dto.EmployeeInformationDto;
import com.example.company.dto.InformationDto;
import com.example.company.dto.RegistrDto;
import com.example.company.entity.*;

import com.example.company.entity.enums.Rolename;
import com.example.company.repository.*;
import com.example.company.response.ApiResponse;
import com.example.company.response.RoleResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuthService implements UserDetailsService {
    @Autowired
    EmployeeRepository employeeRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    JavaMailSender javaMailSender;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    TurniketRepository turniketRepository;
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    SalaryHistoryRepository salaryHistoryRepository;

    // Employeeni ro'yxatdan o'tkazish
    public ApiResponse registr(RegistrDto registrDto) {
        if (getRoleAuth().isStatus()) {
            Optional<Employee> byEmail = employeeRepository.findByEmail(registrDto.getEmail());
            if (!byEmail.isPresent()) {
                Optional<Role> byId = roleRepository.getRole(registrDto.getRole_id());
                if (byId.isPresent()) {
                    String roleChoice = byId.get().getAuthority();
                    String employeesRoleInSystem = getRoleAuth().getRole().getAuthority();
                    String director=Rolename.ROLE_DIRECTOR.name();
                    String hrmanager=Rolename.ROLE_HR_MANAGER.name();
                    String staff=Rolename.ROLE_STAFF.name();
                    if((roleChoice.equals(hrmanager)&&employeesRoleInSystem.equals(director))||
                        (roleChoice.equals(director)&&employeesRoleInSystem.equals(director))||
                           (roleChoice.equals(staff)&&employeesRoleInSystem.equals(director))||
                            (roleChoice.equals(staff)&&employeesRoleInSystem.equals(hrmanager))){
                        String emailCode = UUID.randomUUID().toString();
                        Employee employee = new Employee(registrDto.getFirtsname(), registrDto.getLastname(),
                                Collections.singleton(byId.get()), registrDto.getSalary(),
                                emailCode, registrDto.getEmail(), passwordEncoder.encode(registrDto.getPassword()));
                        employeeRepository.save(employee);
                        boolean b = sendEmail(registrDto.getEmail(), emailCode);
                        if (b) {
                            return new ApiResponse("Emailga kod jo'natildi", true);
                        } else {
                            return new ApiResponse("Kod jo'natilmadi", false);
                        }
                    } else {
                        return new ApiResponse("Sizda bu operatsiyani bajarishga huquq yo'q", false);
                    }
                } else {
                    return new ApiResponse("Bunday rol topilmadi", false);
                }
            } else {
                return new ApiResponse("Bunday email mavjud", false);
            }
        } else {
            return new ApiResponse("Sizda bu operatsiyani bajarishga huquq yo'q", false);
        }
    }

    //Xodim korxonaga kirishi
    public ApiResponse turniketIncome(String username) {
        Optional<Employee> optional = employeeRepository.findByEmail(username);
        if (optional.isPresent()) {
            Turniket turniket = new Turniket();
            turniket.setIncome(true);
            turniket.setEmployee(optional.get());
            turniketRepository.save(turniket);
            return new ApiResponse("Xodim korxonaga kirdi", true);
        } else {
            return new ApiResponse("Korxonada bunday xodim topilmadi", false);
        }
    }

    //Xodimni korxonadan chiqishi
    public ApiResponse turniketOutcome(String username) {
        Optional<Employee> optional = employeeRepository.findByEmail(username);
        if (optional.isPresent()) {
            Turniket turniket = new Turniket();
            turniket.setIncome(true);
            turniket.setEmployee(optional.get());
            turniketRepository.save(turniket);
            return new ApiResponse("Xodim korxonadan chiqdi", true);
        } else {
            return new ApiResponse("Korxonada bunday xodim topilmadi", false);
        }
    }

    // Emailga xabar jo'natish
    public boolean sendEmail(String sendingEmail, String emailCode) {
        try {
            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setFrom("Faxriyor");
            simpleMailMessage.setTo(sendingEmail);
            simpleMailMessage.setSubject("Tasdqiqlash kodi:");
            simpleMailMessage.setText("<a href='http://localhost:8080/api/auth/verifyemail?emailCode=" + emailCode + "&sendingEmail=" + sendingEmail + "'>Tasdiqlang</a>");
            javaMailSender.send(simpleMailMessage);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<Employee> optional = employeeRepository.findByEmail(email);
        return optional.orElseThrow(() -> new UsernameNotFoundException("Topilmadi"));
    }

    public boolean sendEmail(String sendingEmail, UUID uuid) {
        try {
            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setFrom("Faxriyor");
            simpleMailMessage.setTo(sendingEmail);
            simpleMailMessage.setSubject("Tasdqiqlash kodi:");
            simpleMailMessage.setText("<a href='http://localhost:8080/api/auth/taskattachconfirm?uuid=" + uuid + "&sendingEmail=" + sendingEmail + "'>Tasdiqlang</a>");
            javaMailSender.send(simpleMailMessage);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Xodimlar ro'yxatini olish
    public List<Employee> getEmployee() {
        if (getRoleAuth().isStatus()){
            return employeeRepository.findByRole(roleRepository.findByRolename(Rolename.ROLE_STAFF));
        } else {
            return new ArrayList<>();
        }
    }

    //Xodimlarni kelib ketishi va bajargan vazifalari
    public ResponseEntity<?> employeeInformation(EmployeeInformationDto informationDto) {
        if (getRoleAuth().isStatus()) {
            Optional<Turniket> optional = turniketRepository.findByTimeAfterAndTimeBeforeAndEmployee_Email(informationDto.getTime(), informationDto.getTime2(), informationDto.getUsername());
            if (optional.isPresent()) {
                Optional<Task> optional1 = taskRepository.findByEmployeeContainingAndDeadlineAfterAndDeadlineBefore(employeeRepository.findByEmail(informationDto.getUsername()).get(), informationDto.getTime(), informationDto.getTime2());
                if (optional1.isPresent()) {
                    return ResponseEntity.ok(new InformationDto(optional1.get(), optional.get()));
                } else {
                    return ResponseEntity.ok(new InformationDto());
                }
            } else {
                return ResponseEntity.ok(new InformationDto());
            }
        } else {
            return ResponseEntity.status(403).body("Sizda bu huquq yo'q");
        }
    }
    // Xodim bo'yicha oyliklarni ko'rish
    public ResponseEntity<?> getSalaryByEmployee(String username){
        if (getRoleAuth().isStatus()) {
            Optional<SalaryHistory> optional = salaryHistoryRepository.findByEmployee_Email(username);
            return ResponseEntity.ok(optional.orElseThrow(() -> new UsernameNotFoundException("Bunday xodim topilmadi")));
        }else {
            return ResponseEntity.status(403).body("Sizda bu huquq yo'q");
        }
    }

    public RoleResponse getRoleAuth(){
        Employee employee = (Employee) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Set<Role> role = employee.getRole();
        Role roleDirector = roleRepository.findByRolename(Rolename.ROLE_DIRECTOR);
        Role roleHrManager = roleRepository.findByRolename(Rolename.ROLE_HR_MANAGER);
        if (role.contains(roleDirector)){
            return new RoleResponse(roleDirector,true);
        }else {
            if (role.contains(roleHrManager)){
                return new RoleResponse(roleHrManager,true);
            }else {
                return new RoleResponse(null,false);
            }
        }
    }
}