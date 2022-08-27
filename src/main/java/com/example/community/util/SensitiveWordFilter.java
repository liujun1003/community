package com.example.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveWordFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveWordFilter.class);

    // 替换符
    private static final String REPLACEMENT = "***";

    // 前缀树的根节点
    private final TrieNode rootNode = new TrieNode();

    // 根据敏感词文件构建敏感词前缀树
    @PostConstruct
    public void init() {
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-word.txt");
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
        ) {
            String keyWord;
            while ((keyWord = br.readLine()) != null) {
                // 添加敏感词到前缀树
                this.addKeyWord(keyWord);
            }
        } catch (IOException e) {
            logger.error("读取敏感词文件失败！" + e.getMessage());
        }

    }

    // 添加一个敏感词到前缀树中
    private void addKeyWord(String keyWord) {
        // 定义一个指针指向rootNode
        TrieNode tempNode = rootNode;

        // 逐个字符开始添加
        for (int i = 0; i < keyWord.length(); i++) {
            Character c = keyWord.charAt(i);
            TrieNode subNode = new TrieNode();

            tempNode.addSubTrieNode(c, subNode);
            tempNode = subNode;
            if (i == keyWord.length() - 1) {
                subNode.setKeyWordEnd(true);
            }
        }
    }

    /**
     * 过滤一个字符串中的敏感词
     * @param keyWord 待过滤字符串
     * @return 结果字符串
     */
    public String filterSensitiveWord(String keyWord) {
        if (StringUtils.isBlank(keyWord)) {
            return null;
        }

        // 前缀树指针
        TrieNode tempNode = rootNode;

        // 过滤词首指针
        int begin = 0;
        //过滤词操作指针
        int position = 0;

        // 结果字符串
        StringBuilder sb = new StringBuilder();

        // 开始过滤，position指针到过滤词结尾，处理过程结束
        while (position < keyWord.length()) {
            Character c = keyWord.charAt(position);

            // 跳过符号
            if (this.isSymbol(c)) {
                // 前缀树指针位于根节点，将当前字符计入结果字符串，begin后移一位
                if (tempNode == rootNode) {
                    sb.append(c);
                    begin++;
                }
                // position后移一位
                position++;
                continue;
            }

            // 匹配当前操作字符与前缀树子节点
            if (tempNode.getSubTrieNode(c) == null) {
                // 当前操作字符不在前缀树中
                sb.append(keyWord.charAt(begin));
                position = ++begin;
                tempNode = rootNode;
            } else if (!tempNode.getSubTrieNode(c).isKeyWordEnd) {
                // 当前操作字符在前缀树中的非叶子节点
                position++;
                tempNode = tempNode.getSubTrieNode(c);
            } else {
                // 当前操作字符在前缀树中的叶子节点
                begin = ++position;
                tempNode = rootNode;
                sb.append(REPLACEMENT);
            }
        }

        // 加上剩余的字符串
        sb.append(keyWord.substring(begin));
        return sb.toString();
    }

    // 判断一个字符是否是符号
    private boolean isSymbol(Character c) {
        // 0x2E80~0x9FFF 是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }


    // 前缀树的节点类
    private static class TrieNode {

        // 是否叶子节点的标志，叶子节点表示一个敏感词的结尾
        private boolean isKeyWordEnd = false;

        // 子节点集合
        private final Map<Character, TrieNode> subTrieNodes = new HashMap<>();

        public boolean isKeyWordEnd() {
            return isKeyWordEnd;
        }

        public void setKeyWordEnd(boolean keyWordEnd) {
            isKeyWordEnd = keyWordEnd;
        }

        // 获取子节点
        public TrieNode getSubTrieNode(Character c) {
            return subTrieNodes.get(c);
        }

        // 添加子节点
        public void addSubTrieNode(Character c, TrieNode trieNode) {
            subTrieNodes.put(c, trieNode);
        }
    }
}
