package com.example.img2dcm_andriod;


import org.dcm4che3.data.*;
import org.dcm4che3.util.UIDUtils;

import java.nio.file.*;


public class ImageToDicomService {
    private static final ElementDictionary DICT = ElementDictionary.getStandardElementDictionary();
    private static final int[] TYPE2_TAGS ={
            Tag.ContentDate,
            Tag.ContentTime
    };


    /**
     *
     * Set metadata to DICOM Tag
     *
     * @param srcPath The JPG file to read from.
     * @param desPath The DICOM file to read from.
     * @return none
     */
    public void convertJpg2Dcm(String srcPath, String desPath )throws Exception  {

        //Add attributes to DICOM tags
        Attributes staticMetaData = new Attributes();
        initMetaData(staticMetaData);


        Path src = Paths.get(srcPath);
        Path dest = Paths.get(desPath);

        Jpg2DcmUtil.convert(src,dest,staticMetaData);

    }

    private void initMetaData(Attributes staticMetaData){
        //set UID
        staticMetaData.setString(Tag.StudyInstanceUID,VR.UI, UIDUtils.createUID());
        staticMetaData.setString(Tag.SeriesInstanceUID,VR.UI,UIDUtils.createUID());
        staticMetaData.setString(Tag.SOPInstanceUID,VR.UI,UIDUtils.createUID());

        //patient info
        setMetaData(staticMetaData,Tag.PatientName,"Test Patient");
        setMetaData(staticMetaData,Tag.PatientID,"Test Patient");
        setMetaData(staticMetaData,Tag.PatientSex,"M");
        setMetaData(staticMetaData,Tag.PatientAge,"50");
        setMetaData(staticMetaData,Tag.PatientBirthDate,"19500101");

        //study info
        setMetaData(staticMetaData,Tag.StudyDate,"20100101");
        setMetaData(staticMetaData,Tag.StudyTime,"123");
        setMetaData(staticMetaData,Tag.StudyDescription,"Study Description");
        setMetaData(staticMetaData,Tag.StudyID,"123");

        //series info
        setMetaData(staticMetaData,Tag.SeriesDate,"20100101");
        setMetaData(staticMetaData,Tag.SeriesTime,"123");


        //number info
        setMetaData(staticMetaData,Tag.SeriesNumber,"999");
        setMetaData(staticMetaData,Tag.InstanceNumber,"1");

        setMetaData(staticMetaData,Tag.SOPClassUID,UID.SecondaryCaptureImageStorage);
        supplementType2Tags(staticMetaData);

    }

    private void setMetaData(Attributes metaData,int tag,String value){
        if(value!=null){
            metaData.setString(tag,DICT.vrOf(tag),value);
        }
    }

    private void supplementType2Tags(Attributes metaData){
        for(int tag:TYPE2_TAGS)
            if(!metaData.contains(tag))
                metaData.setNull(tag,DICT.vrOf(tag));
    }

}
