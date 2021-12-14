package train.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import train.entity.TrainType;
import train.service.TrainService;

@Component
public class InitData implements CommandLineRunner {

    @Autowired
    TrainService service;

    @Override
    public void run(String... args) throws Exception {
        TrainType info = new TrainType();

        info.setId("GaoTieOne");
        info.setConfortClass(4);
        info.setEconomyClass(4);
        info.setAverageSpeed(250);
        service.create(info, null);

        info.setId("GaoTieTwo");
        info.setConfortClass(4);
        info.setEconomyClass(4);
        info.setAverageSpeed(200);
        service.create(info, null);

        info.setId("DongCheOne");
        info.setConfortClass(4);
        info.setEconomyClass(4);
        info.setAverageSpeed(180);
        service.create(info, null);

        info.setId("ZhiDa");
        info.setConfortClass(4);
        info.setEconomyClass(4);
        info.setAverageSpeed(120);
        service.create(info, null);

        info.setId("TeKuai");
        info.setConfortClass(4);
        info.setEconomyClass(4);
        info.setAverageSpeed(120);
        service.create(info, null);

        info.setId("KuaiSu");
        info.setConfortClass(4);
        info.setEconomyClass(4);
        info.setAverageSpeed(90);
        service.create(info, null);
    }
}
