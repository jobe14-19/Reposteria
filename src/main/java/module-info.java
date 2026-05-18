module com.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires java.xml;
    requires com.microsoft.sqlserver.jdbc;
    requires jasperreports;
    requires java.mail;
    opens com.example.demo to javafx.fxml;
    opens com.example.demo.controller to javafx.fxml;
    exports com.example.demo;
    exports com.example.demo.controller;
    exports com.example.demo.model;
    exports com.example.demo.dao;
    exports com.example.demo.service;
    exports com.example.demo.util;
}
