package com.tick42.quicksilver.controllers;

import com.sun.org.apache.regexp.internal.RE;
import com.tick42.quicksilver.models.Extension;
import com.tick42.quicksilver.services.base.ExtensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping(value = "/api/extension")
public class ExtensionController {

    private final ExtensionService extensionService;

    @Autowired
    public ExtensionController(ExtensionService extensionService) {
        this.extensionService = extensionService;
    }

    @GetMapping(value = "/all")
    public List<Extension> findAll() {
        return extensionService.findAll();
    }

    @GetMapping(value = "/get/{id}")
    public Extension findById(@PathVariable(name = "id") int id) {
        return extensionService.findById(id);
    }

    @GetMapping(value = "/search/{searchQuery}")
    public List<Extension> findByName(@PathVariable(name = "searchQuery") String searchQuery) {
        System.out.println(searchQuery);
        return extensionService.findByName(searchQuery);
    }

    @PostMapping(value = "/add")
    @ResponseBody
    public Extension addExtension(@RequestBody Extension extension) {
        System.out.println(extension.getIsPending());
        extensionService.create(extension);
        return extension;
    }
}
