package com.skillgap.skillgap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.skillgap.skillgap.model.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

}