package com.nowcoder.community.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * @Author: kraken
 * @Date: 2021/4/19 22:01
 */
public class SensitiveWordUtils {

    // 记录日志
    private static final Logger logger = LoggerFactory.getLogger(SensitiveWordUtils.class);

    // 文件的字符编码
    private String ENCODING = "UTF-8";
    // 敏感词相对的路径
    private String PATH_RELATIVE = "/src/main/resourcec/sensitive-words.txt";
    // 敏感词汇树
    private HashMap sensitiveWordMap;
    // 包含敏感词的最多字数
    private Integer MAX_CONTAIN = 2;
    // 最小匹配规则
    public static int minMatchTYPE = 1;
    // 最大匹配规则
    public static int maxMatchTYPE = 2;

    /**
     * 获取文本中敏感词的个数
     * @param txt 要检测的文本
     * @param beginIndex 开始的地方
     * @param matchType 匹配的规则
     * @return  敏感词的长度
     */
    public Integer getSensitiveWordNums(String txt, int beginIndex, int matchType) {
        Set<String> wordSet =readSensitiveWordFromFile();
        addSensitiveWordToHashMap(wordSet);
        Integer result=0;
        for(int i = 0 ; i < txt.length() ; i++){
            //判断是否包含敏感字符
            int length = CheckSensitiveWord(txt, i, matchType);
            if(length > 0){    //存在,加入list中
                result++;
                i = i + length - 1;
            }
        }
        return result;
    }

    /**
     *
     * @param txt 检测的文本
     * @param matchType 匹配的规则
     * @return Set<String> 敏感词汇集合
     */
    public Set<String> getSensitiveWordSet(String txt , int matchType){
        Set<String> wordSet = readSensitiveWordFromFile();
        addSensitiveWordToHashMap(wordSet);
        return getSensitiveWord(txt,matchType);
    }

    /**
     *
     * @param txt
     * @param matchType
     * @param replaceChar 替换的词
     * @return 处理后的文本
     */
    public String replaceAllSensitiveWord(String txt,int matchType,String replaceChar){
        Set<String> wordSet = readSensitiveWordFromFile();
        addSensitiveWordToHashMap(wordSet);
        return replaceSensitiveWord(txt,matchType,replaceChar);
    }

    /**
     * 从文本中以获取敏感词
     * @return  敏感词的Set集合
     */
    private Set<String> readSensitiveWordFromFile() {
        Set<String> wordSet = null;
        //获取项目路径
        String projectPath = System.getProperty("user.dir");
        //拼接得到敏感词的路径
        String path = projectPath + PATH_RELATIVE;
        File file = new File(path);
        try(    // br用于读取文件的每一行
                InputStreamReader reader = new InputStreamReader(new FileInputStream(file),ENCODING);
                // br用于读取文件的每一行
                BufferedReader br =new BufferedReader(reader);
                ) {
            if (file.isFile() && file.exists()) {
                // 初始化wordSet
                wordSet = new HashSet<String>();
                String txt = null;
                while((txt = br.readLine()) != null) {
                    wordSet.add(txt);
                }
            } else {
                throw new Exception("文件错误或文件不存在！");
            }
        } catch (Exception e) {
            logger.error("加载敏感词文件失败: " + e.getMessage());
        }
        return wordSet;
    }

    /**
     * 将敏感词汇处理成敏感词汇树
     * @param keyWordSet 敏感词汇表
     */
    private void addSensitiveWordToHashMap(Set<String> keyWordSet) {
        sensitiveWordMap = new HashMap(keyWordSet.size());
        // 读取每一个关键词
        String key = null;
        // 当前处理的map
        Map nowMap = null;
        // 新建的map
        Map<String, String> newWordMap = null;
        // 用keyWordSet迭代器进行遍历
        Iterator<String> iterator = keyWordSet.iterator();
        while(iterator.hasNext()) {
            key = iterator.next();
            nowMap = sensitiveWordMap;
            // 遍历敏感词的每一位
            for (int i = 0; i < key.length(); i++) {
                // 将一位转换为char类型
                char keyChar = key.charAt(i);
                Object wordMap = nowMap.get(keyChar);
                // 如果存在该key，直接赋值
                if (wordMap != null) {
                    nowMap = (Map) wordMap;
                } else {
                    // 不存在则构建一个map，同时将isEnd设置为 0
                    newWordMap = new HashMap<String, String>();
                    // 不是最后一个
                    newWordMap.put("isEnd","0");
                    nowMap.put(keyChar,newWordMap);
                    nowMap = newWordMap;
                }
                if (i == key.length() -1) {
                    nowMap.put("isEnd", "1");    //最后一个
                }
            }
        }
    }

    /**
     * 获取文本中敏感词汇的个数
     * @param txt 要检测的文本
     * @param beginIndex 开始的地方
     * @param matchType 匹配的规则
     * @return 敏感词汇的长度
     */
    private int CheckSensitiveWord(String txt,int beginIndex,int matchType){
        boolean  flag = false;
        int matchFlag = 0;     //匹配标识数默认为0
        Map nowMap = sensitiveWordMap;
        char word = 0;
        for(int i = beginIndex; i < txt.length() ; i++){
            word = txt.charAt(i);
            nowMap = (Map) nowMap.get(word);//获取指定key
            if(nowMap != null){
                matchFlag++;     //找到相应key，匹配标识+1
                if("1".equals(nowMap.get("isEnd"))){       //如果为最后一个匹配规则,结束循环，返回匹配标识数
                    flag = true;       //结束标志位为true
                    if(minMatchTYPE == matchType){    //最小规则，直接返回,最大规则还需继续查找
                        break;
                    }
                }
            }else{
                break;
            }
        }
        return (matchFlag>=MAX_CONTAIN&&flag)?matchFlag:0;
    }
    /**
     *
     * @param txt 检测的文本
     * @param matchType 匹配的规则
     * @return Set<String> 敏感词汇集合
     */
    private Set<String> getSensitiveWord(String txt , int matchType){
        Set<String> sensitiveWordList = new HashSet<String>();
        for(int i = 0 ; i < txt.length() ; i++){
            //判断是否包含敏感字符
            int length = CheckSensitiveWord(txt, i, matchType);
            if(length > 0){    //存在,加入list中
                sensitiveWordList.add(txt.substring(i, i+length));
                i = i + length - 1;
            }
        }
        return sensitiveWordList;
    }
    //判断文字是否包含敏感字符

    /**
     * 检测是否包含敏感词
     * @param txt 检测的文本
     * @param matchType 匹配的规则
     * @return
     */
    public boolean isContaintSensitiveWord(String txt,int matchType){
        boolean flag = false;
        for(int i = 0 ; i < txt.length() ; i++){
            //判断是否包含敏感字符
            int matchFlag = this.CheckSensitiveWord(txt, i, matchType);
            //存在
            if(matchFlag > 0){
                flag = true;
            }
        }
        return flag;
    }

    /**
     *
     * @param txt
     * @param matchType
     * @param replaceChar 替换的词
     * @return 处理后的文本
     */
    private String replaceSensitiveWord(String txt,int matchType,String replaceChar){
        String resultTxt = txt;
        Set<String> set = getSensitiveWord(txt, matchType);
        Iterator<String> iterator = set.iterator();
        String word = null;
        String replaceString = null;
        while (iterator.hasNext()) {
            word = iterator.next();
            replaceString = getReplaceChars(replaceChar, word.length());
            resultTxt = resultTxt.replaceAll(word, replaceString);
        }

        return resultTxt;
    }

    //替换原本字符串
    private String getReplaceChars(String replaceChar,int length){
        String resultReplace = replaceChar;
        for(int i = 1 ; i < length ; i++){
            resultReplace += replaceChar;
        }
        return resultReplace;
    }

}
