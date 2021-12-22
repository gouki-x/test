package densowave.com.DensoScannerSDK_demo;

public class ShisanInfo {

    String epcID;
    String assetName;
    String location;
    String result;

    public ShisanInfo(String epcID, String assetName, String location) {
        this.epcID = epcID;
        this.assetName = assetName;
        this.location = location;
        this.result = "N/A";
    }

    public String getEpcID() {
        return epcID;
    }

    public void setEpcID(String epcID) {
        this.epcID = epcID;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }


}
