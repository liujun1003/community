package com.example.community.service;

import com.example.community.dao.elasticsearch.DiscussPostRepository;
import com.example.community.entity.DiscussPost;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

@Service
public class ElasticsearchService {

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    /**
     * 保存DiscussPost到Elasticsearch对应的index中
     * @param discussPost
     */
    public void saveDiscussPost(DiscussPost discussPost) {
        discussPostRepository.save(discussPost);
    }

    /**
     * 通过DiscussPost的id删除Elasticsearch中的对应数据
     * @param id
     */
    public void deleteDiscussPostById(int id) {
        discussPostRepository.deleteById(id);
    }

    /**
     * 通过keyword搜索相关的帖子，并将相关帖子中的keyword文字高亮显示，返回帖子列表
     *
     * @param keyword 搜索关键字
     * @param current 分页显示的的当前页
     * @param limit   分页显示的每页条目数
     * @return 搜索到的帖子列表
     */
    public SearchHits<DiscussPost> searchDiscusPostByKeyword(String keyword, int current, int limit) {
        QueryBuilder queryBuilder = keyword.isEmpty() ? null : QueryBuilders.multiMatchQuery(keyword, "title", "content");

        // 构建复杂的查询条件
        // 使用NativeSearchQuery，可以实现构造Query、Sort、Page、Highlight等条件
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder)
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(current, limit))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                )
                .build();


        // 使用Spring Data提供的ElasticsearchRestTemplate对象查询
        return elasticsearchRestTemplate.search(nativeSearchQuery, DiscussPost.class);
    }
}
