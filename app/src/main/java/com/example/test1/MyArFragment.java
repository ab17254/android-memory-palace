package com.example.test1;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

public class MyArFragment extends ArFragment {

    /**
     * AR Fragment class
     * Contains the  method for the configuration of the ARFragment
     * @param session AR session
     * @return config
     */
    @Override
    protected Config getSessionConfiguration(Session session) {
//      Hides the instruction animation which covers the UI
        getPlaneDiscoveryController().setInstructionView(null);
        Config config = super.getSessionConfiguration(session);
        config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
        return config;
    }
}