package com.example.community.controller;

import com.example.community.entity.DiscussPost;
import com.example.community.entity.Page;
import com.example.community.service.ElasticsearchService;
import com.example.community.service.LikeService;
import com.example.community.service.UserService;
import com.example.community.util.CommunityContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ElasticsearchController implements CommunityContent {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String getSearch(String keyword, Page page, Model model) {
        // 设置分页信息
        page.setPath("/search/?keyword=" + keyword);
        page.setLimit(5);

        // 查询帖子
        SearchHits<DiscussPost> searchHits = elasticsearchService.searchDiscusPostByKeyword(keyword, page.getCurrent() - 1, page.getLimit());
        page.setRows((int) searchHits.getTotalHits());

        // 处理查询结果集SearchHits
        List<Map<String, Object>> postVo = new ArrayList<>();

        for (SearchHit<DiscussPost> searchHit : searchHits) {
            DiscussPost discussPost = searchHit.getContent();
            // 处理高亮文本
            Map<String, List<String>> highlightFields = searchHit.getHighlightFields();
            List<String> highlight = highlightFields.get("title");
            if (highlight != null) {
                discussPost.setTitle(highlight.get(0));
            }
            highlight = highlightFields.get("content");
            if (highlight != null) {
                discussPost.setContent(highlight.get(0));
            }

            // 聚合帖子视图信息
            Map<String, Object> map = new HashMap<>();
            map.put("post", discussPost);
            map.put("user", userService.findUserById(discussPost.getUserId()));
            map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPost.getId()));

            postVo.add(map);
        }

        model.addAttribute("postVO", postVo);
        model.addAttribute("keyword", keyword);

        return "site/search";
    }

}
