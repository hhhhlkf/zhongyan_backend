package com.gosling.bms.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @ClassName:FileUtil
 * @Description: TODO
 * @Author:Dazz1e
 * @Date:2022/5/22 下午 4:10
 * Version V1.0
 */
@Component
@Slf4j
public class FileUtil {
    public final static String AVATAR_PATH = "static/uploads/images/employee/avatars/";
    public String uploadFile(MultipartFile file, String basePath) throws Exception
    {
        if (file.isEmpty()) {
            throw new Exception();
        }
        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String fileName = com.gosling.bms.utils.StringUtil.allocateUuid() + suffix;//服务端文件
        try {
            //Linux环境需修改
            ApplicationHome h = new ApplicationHome(getClass());
            File jarF = h.getSource();
            File path = new File(jarF.getParentFile().toString());
            //File path = new File("./");
            //log.info(path.getAbsolutePath());
            if (!path.exists()) {
                path = new File("");
            }
            File upload = new File(path.getAbsolutePath(), basePath);
            if (!upload.exists()) {
                upload.mkdirs();
            }
            String uploadPath = upload + "/";

            file.transferTo(new File(uploadPath + fileName));
            return basePath + fileName;
        }catch (Exception e)
        {
            throw new Exception();
        }
    }
    public void removeFile(String path) {
        File file = new File(path);
        //log.info(path);
        file.delete();
    }
}

