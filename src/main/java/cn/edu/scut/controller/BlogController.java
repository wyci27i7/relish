package cn.edu.scut.controller;

import cn.edu.scut.common.result.Result;
import cn.edu.scut.entity.Blog;
import cn.edu.scut.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        blogService.saveBlog(blog);
        return Result.ok();
    }

    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(@RequestParam("lastId") Long max,
                                    @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        Map<String, Object> result = blogService.queryBlogOfFollow(max, offset);
        return Result.ok(result);
    }
}
