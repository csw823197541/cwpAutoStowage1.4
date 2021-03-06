package generateResult

import autoStow.CallAutoStow
import groovy.json.JsonSlurper
import importDataInfo.*
import importDataProcess.AutoStowInputProcess
import utils.FileUtil

/**
 * Created by leko on 2016/1/22.
 */
public class GenerateAutoStowResult {

    //调用自动配载
    public
    static List<AutoStowResultInfo> getAutoStowResult(List<GroupInfo> groupInfoList, List<ContainerInfo> containerInfoList, List<ContainerAreaInfo> containerAreaInfoList, List<PreStowageData> preStowageDataList, List<CwpResultMoveInfo> cwpResultMoveInfoList) {
        List<AutoStowResultInfo> autoStowResultInfoList = new ArrayList<AutoStowResultInfo>();

        //处理在场箱信息
        String containerStr = AutoStowInputProcess.getContainerJsonStr(containerInfoList);

        //处理箱区信息
        String containerAreaStr = AutoStowInputProcess.getContainerAreaJsonStr(containerAreaInfoList);

        //处理预配信息
        String preStowageStr = AutoStowInputProcess.getPreStowageJsonStr(preStowageDataList);

//        try {//将自动配载要用的结果写在文件里，让算法去读这个文件
//            FileUtil.writeToFile("toAutoStowJsonData/Container.txt", containerStr);
//            FileUtil.writeToFile("toAutoStowJsonData/PreStowage.txt", preStowageStr);
//            FileUtil.writeToFile("toAutoStowJsonData/ContainerArea.txt", containerAreaStr);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        //处理cwp输出信息
        String cwpResultStr = AutoStowInputProcess.getCwpResultJsonStr(cwpResultMoveInfoList);

//        try {//将自动配载要用的结果写在文件里
//            FileUtil.writeToFile("toAutoStowJsonData/CwpOutput.txt", cwpResultStr);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        String autoStowStr = null;
        if (containerStr != null && containerAreaStr != null && preStowageStr != null && cwpResultStr != null) {
            //调用自动配载算法
            autoStowStr = CallAutoStow.autoStow(containerStr, containerAreaStr, preStowageStr, cwpResultStr);

//            try {//将自动配载要用的结果写在文件里
//                FileUtil.writeToFile("toAutoStowJsonData/autoStowResult.txt", autoStowStr);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

            System.out.println("自动配载算法返回的结果：" + autoStowStr);
            if (autoStowStr != null) {
                if(!"loadDataError".equals(autoStowStr)) {
                    autoStowResultInfoList = getAutoStowResult(autoStowStr, preStowageDataList, containerInfoList);
                }
            } else {
                System.out.println("自动配载算法没有返回结果！");
            }
        } else {
            System.out.println("自动配载算法需要的4个参数信息中有空的，不能调用算法！");
        }
        return autoStowResultInfoList;
    }


    public
    static List<AutoStowResultInfo> getAutoStowResult(String autoStowStr, List<PreStowageData> preStowageDataList, List<ContainerInfo> containerInfoList) {

        List<AutoStowResultInfo> autoStowResultInfoList = new ArrayList<>();
        boolean isError = false;

        try {

            Long voyId = containerInfoList.get(0).getIYCVOYID().longValue();//根据在场箱得到航次？

            //将预配信息进行处理，根据船箱位得到卸船的箱号信息
            Map<String, PreStowageData> preStowageDataMapL = new HashMap<>();
            for (PreStowageData preStowageData : preStowageDataList) {
                String bayId = preStowageData.getVBYBAYID();    //倍号
                String rowId = preStowageData.getVRWROWNO();    //排号
                String tieId = preStowageData.getVTRTIERNO();   //层号
                String vp = bayId + rowId + tieId;
                if ("L".equals(preStowageData.getLDULD())) {
                    if (!preStowageDataMapL.containsKey(vp)) {
                        preStowageDataMapL.put(vp, preStowageData);
                    }
                }
            }

            def root = new JsonSlurper().parseText(autoStowStr);
            assert root instanceof List//根据读入数据的格式，可以直接把json转换成List

            root.each { autoStowResult ->
                AutoStowResultInfo autoStowResultInfo = new AutoStowResultInfo();
                assert autoStowResult instanceof Map
                String[] vesselLoc = autoStowResult.vesselLoc.split("%");
                String containerID = autoStowResult.containerID;
                String yardLoc = autoStowResult.yardLoc;
                String unStowedReason = yardLoc;//场箱位记录未配载的原因
                String vesselPosition = vesselLoc[1] + "" + vesselLoc[2] + "" + vesselLoc[3];//key,船上的位置：倍排层
                String size = "";
                if (preStowageDataMapL.get(vesselPosition) != null) {
                    size = preStowageDataMapL.get(vesselPosition).getSIZE();
                }
                if (containerID == null) {
                    containerID = "?";
                    yardLoc = "?";
                }
                if(!yardLoc.startsWith("unStowed")) {
//                    yardLoc = yardLoc.split("%")[0] + "" + yardLoc.split("%")[1] + "" + yardLoc.split("%")[2] + "" + yardLoc.split("%")[3];
                yardLoc = yardLoc.replace("%", "");
                }
                autoStowResultInfo.setAreaPosition(yardLoc);
                autoStowResultInfo.setSize(size);
                autoStowResultInfo.setUnitID(containerID);
                autoStowResultInfo.setVesselPosition(vesselPosition);
                autoStowResultInfo.setVoyId(voyId);
                autoStowResultInfo.setUnStowedReason(unStowedReason);
                autoStowResultInfoList.add(autoStowResultInfo);
            }
        } catch (Exception e) {
            System.out.println("自动配载返回结果数据解析时，发现json数据异常！")
            isError = true;
            e.printStackTrace()
        }
        if (isError) {
            System.out.println("自动配载返回结果数据解析失败！")
            return null;
        } else {
            System.out.println("自动配载返回结果数据解析成功！")
            return autoStowResultInfoList
        }
    }
}