package cn.edu.scut.controller;

import cn.edu.scut.common.result.Result;
import cn.edu.scut.dto.LoginFormDTO;
import cn.edu.scut.dto.SendCodeDTO;
import cn.edu.scut.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping("/code")
    public Result sendCode(@Valid SendCodeDTO sendCodeDTO) {
        log.info("手机号码：{}", sendCodeDTO.getPhone());
        return userService.sendCode(sendCodeDTO.getPhone());
    }

    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginFormDTO loginFormDTO) {
        return userService.login(loginFormDTO.getPhone(), loginFormDTO.getCode());
    }

    @GetMapping("/me")
    public Result getUserInfo() {
        return userService.getUserInfo();
    }

}
