INSERT INTO state (
    id,name,"Code__c",country_code,is_active,is_deleted,
    created_at,created_by,updated_at,updated_by
)
VALUES
(uuid_generate_v4(),'Madhya Pradesh','MP','IN',true,false,now(),'SYSTEM',now(),'SYSTEM'),
(uuid_generate_v4(),'Uttarakhand','UK','IN',true,false,now(),'SYSTEM',now(),'SYSTEM'),
(uuid_generate_v4(),'Uttar Pradesh','UP','IN',true,false,now(),'SYSTEM',now(),'SYSTEM'),
(uuid_generate_v4(),'Rajasthan','RJ','IN',true,false,now(),'SYSTEM',now(),'SYSTEM'),
(uuid_generate_v4(),'Maharashtra','MH','IN',true,false,now(),'SYSTEM',now(),'SYSTEM'),
(uuid_generate_v4(),'Himachal','HL','IN',true,false,now(),'SYSTEM',now(),'SYSTEM');

INSERT INTO district (
    id,state_id,name,"Code__c",created_at
)
SELECT uuid_generate_v4(), s.id, v.name, v.code, now()
FROM state s
JOIN (VALUES
('UK','Almora','UK-ALM01'),('UK','Bageshwar','UK-BAG02'),('UK','Chamoli','UK-CHM11'),
('UK','Champawat','UK-CHP03'),('UK','Dehradun','UK-DDN07'),('UK','Haridwar','UK-HRW08'),
('UK','Nainital','UK-NTL04'),('UK','Pauri Garhwal','UK-PRG12'),('UK','Pithoragarh','UK-PTH05'),
('UK','Rudraprayag','UK-RDP13'),('UK','Tehri Garhwal','UK-THG09'),('UK','Udham Singh Nagar','UK-USN06'),
('UK','Uttarkashi','UK-UTK10'),
('UP','Lucknow','UP-LKO01'),('UP','Gautam Buddha Nagar','UP-GBN02'),
('UP','Kanpur Nagar','UP-KNP03'),('UP','Varanasi','UP-VNS04'),('UP','Prayagraj','UP-PRY05'),
('HL','Shimla','HP-SML01'),('HL','Kangra','HP-KNG02'),('HL','Mandi','HP-MND03'),('HL','Solan','HP-SLN04'),
('RJ','Jaipur','RJ-JPR01'),('RJ','Jodhpur','RJ-JDP02'),('RJ','Udaipur','RJ-UDP03'),
('RJ','Kota','RJ-KOT04'),('RJ','Ajmer','RJ-AJM05'),
('MP','Bhopal','MP-BPL01'),('MP','Indore','MP-IDR02'),('MP','Jabalpur','MP-JBP03'),
('MP','Gwalior','MP-GWL04'),('MP','Ujjain','MP-UJN05'),
('MH','Mumbai Suburban','MH-MUM01'),('MH','Pune','MH-PUN02'),
('MH','Nagpur','MH-NGP03'),('MH','Nashik','MH-NSK04'),('MH','Aurangabad','MH-AUR05')
) v(state_code,name,code)
ON s."Code__c"=v.state_code;

INSERT INTO governance_type (
 id,"Code__c",name,country_code,is_active,is_deleted,
 created_at,created_by,updated_at,updated_by
)
VALUES
(uuid_generate_v4(),'ULB','Urban Local Body','IN',true,false,now(),'SYSTEM',now(),'SYSTEM'),
(uuid_generate_v4(),'RLB','Rural Local Body','IN',true,false,now(),'SYSTEM',now(),'SYSTEM'),
(uuid_generate_v4(),'CB','Cantonment Board','IN',true,false,now(),'SYSTEM',now(),'SYSTEM'),
(uuid_generate_v4(),'SA','Special Authority','IN',true,false,now(),'SYSTEM',now(),'SYSTEM');

INSERT INTO governance_sub_type
(id,governance_type_id,"Code__c",name,country_code,is_active,is_deleted,
 created_at,created_by,updated_at,updated_by)
SELECT uuid_generate_v4(),gt.id,v.code,v.name,'IN',true,false,
       now(),'SYSTEM',now(),'SYSTEM'
FROM governance_type gt
JOIN (VALUES
('ULB','ULB-MC','Municipal Corporation'),
('ULB','ULB-MCL','Municipal Council'),
('ULB','ULB-NP','Nagar Panchayat'),
('RLB','RLB-GP','Gram Panchayat'),
('RLB','RLB-BP','Block Panchayat'),
('RLB','RLB-ZP','Zila Parishad'),
('CB','CB-CB','Cantonment Board'),
('SA','SA-DA','Development Authority')
) v(type_code,code,name)
ON gt."Code__c"=v.type_code;

INSERT INTO governing_body
(id,state_id,district_id,governance_sub_type_id,"Code__c",name,is_active,is_deleted,
 created_at,created_by,updated_at,updated_by)
SELECT uuid_generate_v4(),s.id,d.id,gst.id,v.code,v.name,true,false,
       now(),'SYSTEM',now(),'SYSTEM'
FROM (VALUES
('UK','UK-DDN07','ULB-MC','UK-DDN07-MC','Dehradun Municipal Corporation'),
('UK','UK-DDN07','ULB-MCL','UK-DDN07-RMC','Rishikesh Municipal Council'),
('UK','UK-HRW08','ULB-MC','UK-HRW08-MC','Haridwar Municipal Corporation'),
('UK','UK-HRW08','ULB-MCL','UK-HRW08-RMC','Roorkee Municipal Council'),
('UK','UK-HRW08','RLB-GP','UK-HRW08-BGP','Bahadrabad Gram Panchayat'),
('UK','UK-DDN07','RLB-GP','UK-DDN07-RGP','Raipur Gram Panchayat')
) v(state_code,district_code,subtype_code,code,name)
JOIN state s ON s."Code__c"=v.state_code
JOIN district d ON d."Code__c"=v.district_code
JOIN governance_sub_type gst ON gst."Code__c"=v.subtype_code;

INSERT INTO zone (id,governing_body_id,name,created_at)
SELECT uuid_generate_v4(),gb.id,v.name,now()
FROM (VALUES
('UK-DDN07-MC','Dehradun Zone 1'),
('UK-DDN07-MC','Dehradun Zone 2'),
('UK-DDN07-MC','Dehradun Zone 3'),
('UK-DDN07-MC','Dehradun Zone 4'),
('UK-DDN07-MC','Dehradun Zone 5'),
('UK-DDN07-MC','Dehradun Zone 6'),
('UK-DDN07-MC','Dehradun Zone 7'),
('UK-DDN07-MC','Dehradun Zone 8'),
('UK-DDN07-MC','Dehradun Zone 9'),
('UK-HRW08-MC','Haridwar Zone 10'),
('UK-HRW08-MC','Haridwar Zone 11'),
('UK-HRW08-MC','Haridwar Zone 12'),
('UK-HRW08-MC','Haridwar Zone 13'),
('UK-HRW08-MC','Haridwar Zone 14'),
('UK-DDN07-RMC','Rishikesh Zone 15'),
('UK-DDN07-RMC','Rishikesh Zone 16'),
('UK-DDN07-RMC','Rishikesh Zone 17')
) v(gb_code,name)
JOIN governing_body gb ON gb."Code__c"=v.gb_code;
