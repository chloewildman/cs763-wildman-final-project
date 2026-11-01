package edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.User;

public interface UserRepository extends JpaRepository<User, Integer> {

}
