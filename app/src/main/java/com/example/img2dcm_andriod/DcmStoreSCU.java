package com.example.img2dcm_andriod;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.util.SafeClose;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DcmStoreSCU {

    private Connection remote = new Connection();
    private ApplicationEntity ae = new ApplicationEntity("STORESCU");
    private final AAssociateRQ rq = new AAssociateRQ();
    private Association as;
    ExecutorService executorService = null;

    public DcmStoreSCU() {
        initDevice();
        rq.addPresentationContext(new PresentationContext(1,
                UID.Verification, UID.ImplicitVRLittleEndian));
    }

    private void initDevice() {
        Device device = new Device("storescu");
        Connection conn = new Connection();
        device.addConnection(conn);
        device.addApplicationEntity(ae);

        executorService = Executors.newSingleThreadExecutor();
        device.setExecutor(executorService);

        remote.setTlsProtocols(conn.getTlsProtocols());
        remote.setTlsCipherSuites(conn.getTlsCipherSuites());

        ae.addConnection(conn);
    }

    /**
     *
     * Init SCP Server Information
     *
     * @param strAETitle The SCP AETitle.
     * @param strRemoteIP The SCP IP.
     * @param nRemotPort The SCP PORT.
     * @return none
     */
    public void initServerInfo(String strAETitle, String strRemoteIP, int nRemotPort) {
        ae.setAETitle("ANDROIDSCU");
        rq.setCalledAET(strAETitle);
        remote.setHostname(strRemoteIP);
        remote.setPort(nRemotPort);
    }

    /**
     *
     * Send DICOM file to C store
     *
     * @param strFilePath The DICOM file to read from.
     * @return none
     */
    public void sendDicomFile(String strFilePath) throws IOException, InterruptedException, IncompatibleConnectionException, GeneralSecurityException, GeneralSecurityException {
        File file = new File(strFilePath);

        if(!file.exists()) {
            return;
        }

        DicomInputStream in = null;
        try {
            in = new DicomInputStream(file);
            in.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
            Attributes fmi = in.readFileMetaInformation();
            String cuid = fmi.getString(Tag.MediaStorageSOPClassUID);
            String iuid = fmi.getString(Tag.MediaStorageSOPInstanceUID);
            String ts = fmi.getString(Tag.TransferSyntaxUID);

            rq.addPresentationContext(new PresentationContext(rq.getNumberOfPresentationContexts() * 2 + 1, cuid, ts));
            as = ae.connect(remote, rq);

            long fmiEndPos = in.getPosition();
            FileInputStream input = new FileInputStream(file);
            input.skip(fmiEndPos);
            InputStreamDataWriter data = new InputStreamDataWriter(input);
            as.cstore(cuid, iuid, Priority.NORMAL, data, ts, new DimseRSPHandler(as.nextMessageID()));

        } finally {
            close();
            SafeClose.close(in);
        }
    }

    private void close() throws IOException, InterruptedException {
        if(executorService != null) {
            executorService.shutdown();
        }

        if (as != null) {
            if (as.isReadyForDataTransfer())
                as.release();
            as.waitForSocketClose();
        }
    }
}
