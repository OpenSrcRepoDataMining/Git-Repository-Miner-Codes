package CommitKeyWord;

import BarCode.BarCode;
import BarCode.BarInfo;
import BarCode.BarNode;
import Contri.CommitMessages;
import Repository.GitRepository;
import Repository.GitRepositoryFactory;
import com.csvreader.CsvWriter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class CommitPersonDay {
    private Git git;
    private List<CommitMessages> commitMsgs;
    private Map<String, Map<Date, Integer>> person;
    private List<String> personName;
    public int threhold = 100;//限制高于100的人数才能进入名单personName
    Date minDate;   //起始日期
    Date maxDate;   //终止日期
    public CommitPersonDay(Git input_git, int theh){
        git = input_git;
        threhold = theh;
        BuildCommitListAndPerson();
        //barCode = new BarCode(commitMsgs, false, null, null);
    }

    //补充完整BarCode功能实现
    public void BuildCommitListAndPerson (){
        List<RevCommit> commitList = new ArrayList<>();
        Iterable<RevCommit> commits = null;
        try {
            commits = git.log().call();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        for(RevCommit commit:commits){
            commitList.add(commit);
        }
        commitMsgs = new ArrayList<CommitMessages>();
        person = new HashMap<String, Map<Date, Integer>>();
        personName = new ArrayList<>();
        for(int i = 0; i < commitList.size() - 1; i++) {
            RevCommit commit = commitList.get(i);
            CommitMessages msg = new CommitMessages(commit);
            Date date = msg.getCommitTime();
            //FIXME:为了便于统计，强制把时分秒清零了...
            date.setHours(0);
            date.setMinutes(0);
            date.setSeconds(0);
            commitMsgs.add(msg);
            if(minDate==null||maxDate==null){
                minDate = date;
                maxDate = date;
            }else{
                if(date.after(maxDate)){
                    maxDate=date;
                }
                if(date.before(minDate)){
                    minDate=date;
                }
            }
            String id = msg.getAuthorName();
            if (person.containsKey(id)) {
                Map<Date, Integer> mp = person.get(id);
                if(mp.containsKey(msg.getCommitTime())){
                    Integer tmpi = mp.get(msg.getCommitTime());
                    tmpi = tmpi + 1;
                    mp.put(msg.getCommitTime(), tmpi);
                }
                else
                {
                    mp.put(msg.getCommitTime(), new Integer(1));
                }
                person.put(id, mp);
            } else {
                Map<Date, Integer> mp = new HashMap<>();
                mp.put(msg.getCommitTime(), new Integer(1));
                person.put(id, mp);
            }

        }
        for(String entry : person.keySet()){
            Map<Date, Integer> loop = person.get(entry);
            int count = 0;
            for(Integer value:loop.values())
                count += value;
            if(count > threhold)
                personName.add(entry);
        }
        System.out.println("total commit size:" + commitList.size());
    }

    //  按天输出
    public void SaveByDay(String filePath) throws Exception{
        CsvWriter csvWriter = new CsvWriter(filePath, ',', Charset.forName("UTF-8"));
        // 写表头
        String[] header = new String[personName.size() + 2];
        header[0] = "Date";
        header[1] = "All";
        for(int i = 2; i < personName.size() + 2; i++){
            header[i] = personName.get(i-2);
        }
        csvWriter.writeRecord(header);
        Map<Date, Integer> mp = new HashMap<>();
        for(int i = 0;i < commitMsgs.size(); i++){
            CommitMessages commit = commitMsgs.get(i);
            Date dt = commit.getCommitTime();
            if(mp.containsKey(dt)){
                Integer it = mp.get(dt) + 1;
                mp.put(dt, it);
            }
            else{
                mp.put(dt, new Integer(1));
            }
        }
        mp = sortMapByKey(mp);
        for(Map.Entry<Date, Integer> entry: mp.entrySet()){
            Date key = entry.getKey();
            String time = Integer.toString(key.getYear() + 1900) + "/" + Integer.toString(key.getMonth() + 1)+ "/" + Integer.toString(key.getDate());
            //System.out.println("date: "+time);
            csvWriter.write(time);
            csvWriter.write(entry.getValue().toString());
            Date dt = entry.getKey();
            for(String name:personName){
                Map<Date, Integer> oneperson = person.get(name);
                if(oneperson.containsKey(dt))
                {
                    csvWriter.write(oneperson.get(dt).toString());
                }
                else
                    csvWriter.write("0");
            }
            csvWriter.endRecord();
        }
        csvWriter.close();
    }
    //按周输出每个人每天的
    public void SaveByWeek(String filePath) throws Exception{
        CsvWriter csvWriter = new CsvWriter(filePath, ',', Charset.forName("UTF-8"));
        // 写表头
        String[] header = new String[personName.size() + 2];
        header[0] = "Date";
        header[1] = "All";
        for(int i = 2; i < personName.size() + 2; i++){
            header[i] = personName.get(i-2);
        }
        csvWriter.writeRecord(header);
        Date first = getThisWeekMonday(minDate);
        Map<Date, Integer> mp = new HashMap<>();
        for(int i = 0;i < commitMsgs.size(); i++){
            CommitMessages commit = commitMsgs.get(i);
            Date dt = getThisWeekMonday(commit.getCommitTime());
            if(mp.containsKey(dt)){
                Integer it = mp.get(dt) + 1;
                mp.put(dt, it);
            }
            else{
                mp.put(dt, new Integer(1));
            }
        }
        mp = sortMapByKey(mp);
        for(Map.Entry<Date, Integer> entry: mp.entrySet()){
            Date key = entry.getKey();
            String time = Integer.toString(key.getYear() + 1900) + "/" + Integer.toString(key.getMonth() + 1)+ "/" + Integer.toString(key.getDate());
            System.out.println("date: "+key);
            csvWriter.write(time);
            //csvWriter.write(entry.getKey().toString());
            csvWriter.write(entry.getValue().toString());
            Date dt = entry.getKey();
            for(String name:personName){
                Map<Date, Integer> oneperson = person.get(name);
                Integer count = new Integer(0);
                for(Map.Entry<Date, Integer> entry1:oneperson.entrySet()){
                    if(CompareInSameWeek(entry1.getKey(), dt)){
                        count += entry1.getValue();
                    }
                }
                csvWriter.write(count.toString());
            }
            csvWriter.endRecord();
        }
        csvWriter.close();
    }
    public static Date getThisWeekMonday(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        // 获得当前日期是一个星期的第几天
        int dayWeek = cal.get(Calendar.DAY_OF_WEEK);
        if (Calendar.SUNDAY == dayWeek) {//如果是周日，那么要向上取
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        // 设置一个星期的第一天，按中国的习惯一个星期的第一天是星期一
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        // 获得当前日期是一个星期的第几天
        int day = cal.get(Calendar.DAY_OF_WEEK);
        // 根据日历的规则，给当前日期减去星期几与一个星期第一天的差值
        cal.add(Calendar.DATE, cal.getFirstDayOfWeek() - day);
        return cal.getTime();
    }

    public boolean CompareInSameWeek(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        //设置一周的开始,默认是周日,这里设置成星期一
        cal1.setFirstDayOfWeek(Calendar.MONDAY);
        cal2.setFirstDayOfWeek(Calendar.MONDAY);

        cal1.setTime(date1);
        cal2.setTime(date2);

        int subyear = cal1.get(Calendar.YEAR) - cal2.get(Calendar.YEAR);
        if(subyear == 0) {
            return cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR);
        }
        else if(subyear == 1 && cal2.get(Calendar.MONTH) == 11){
            return cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR);
        }
        else if(subyear == -1 && cal1.get(Calendar.MONTH) == 11){
            return cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR);
        }
        return false;
    }
    //Test
    /*public static void main(String args[]) {
        String GitAddress = "https://github.com/Microsoft/microsoft.github.io.git";
//        String GitAddress = "https://github.com/OpenSrcRepoDataDigging/DailyLog.git";
        try {
//            GitRepository repo = new GitRepositoryFactory().cloneRepositoryFrom(GitAddress); //git clone到临时目录
//            GitRepository repo = new GitRepositoryFactory().cloneRepositoryFromTo(GitAddress,"E:\\GitMiner_Codes\\Test"); //git clone到目标目录
            GitRepository repo = new GitRepositoryFactory().openLocalRepositoryFrom("D:\\课程学习\\创新项目\\MyCode\\alluxio\\alluxio"); //获得本地项目的引用
            Git git = repo.getGit();
            if (git == null){
                System.out.println("Git is null");
                return;
            }
            CommitPersonDay cp = new CommitPersonDay(git, 0);
            cp.SaveByWeek("D:\\课程学习\\创新项目\\MyCode\\SaveByWeek.csv");
            cp.SaveByDay("D:\\课程学习\\创新项目\\MyCode\\SaveByDay.csv");
            System.out.println("Ready to cancel");

            //TODO: 测试时为了速度，使用本地项目，但是记得如果用临时目录，要删除
            //repo.deleteRepository();
        }catch (Exception e){
            e.printStackTrace();
        }

    }*/

    //对时间进行排序
    private Map<Date, Integer> sortMapByKey(Map<Date, Integer> map){
        if (map == null || map.isEmpty()) {
            return null;
        }
        Map<Date, Integer> sortMap = new TreeMap<Date, Integer>(new MapKeyComparator());
        sortMap.putAll(map);
        return sortMap;
    }
}
