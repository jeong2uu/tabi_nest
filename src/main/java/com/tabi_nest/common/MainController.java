package com.tabi_nest.common;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
// @RequestMapping("/")
public class MainController {

    @GetMapping("/")
    public String index(Model model){

        System.out.println(">>> MainController index 호출됨");

        // banner 이미지
        List<String> banners = List.of(
                "/images/banner1.jpg",
                "/images/banner2.jpg",
                "/images/banner3.jpg",
                "/images/banner4.jpg",
                "/images/banner5.jpg"
        );

        model.addAttribute("banners", banners);

        return "index";
    }

    @GetMapping("/home")
    public String home(Model model){

        System.out.println(">>> MainController home 호출됨");

        return "home";
    }
}
