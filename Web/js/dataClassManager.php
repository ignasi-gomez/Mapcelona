<?php
require_once('define.php');
require_once('dbManager.php');

class dataClassManager {
    private $db;

    public function __construct(){
        $this->db = new dbManager();
    }

    public function getParameters(){
        // Esto para cuando estén los idiomas
        $sql = "SELECT dc.*, t._value FROM dataclass dc, _translation t, _language l
                WHERE dc.id=t.dataclass_id AND t.language_id=l.id AND l.name=";
        if (isset($_SESSION['lang'])) $sql.="'{$_SESSION['lang']}'";
        else $sql.="'en'";
        //return $this->db->launchQuery('SELECT * FROM dataclass ORDER BY name');
        return $this->db->launchQuery('SELECT * FROM dataclass ORDER BY name');
    }
    // Given a dataclass, the function returns a list of the values that each district has on this dataclass
    public function getDistrictValues($dataclass_id){
        return $this->db->launchQuery("SELECT dis.*, dv._value, dv.age FROM district dis LEFT JOIN district_value dv ON dv.district_id=dis.id AND dv.dataclass_id={$dataclass_id} ORDER BY dis.id");
    }

    public function getNeighbourhoodValues($dataclass_id){
        $maxAge = $this->db->launchQuery("SELECT MAX(nv.age) as age FROM neighbourhood_value nv WHERE nv.dataclass_id={$dataclass_id}");
        $maxAge = $maxAge[0]['age'];
        //print("SELECT ngh.*, nv._value, nv.age FROM neighbourhood ngh LEFT JOIN neighbourhood_value nv ON nv.neighbourhood_id=ngh.id AND nv.dataclass_id={$dataclass_id} AND nv.age={$maxAge} ORDER BY ngh.id");
        return $this->db->launchQuery("SELECT ngh.*, nv._value, nv.age FROM neighbourhood ngh JOIN neighbourhood_value nv ON nv.neighbourhood_id=ngh.id AND nv.dataclass_id={$dataclass_id} AND nv.age={$maxAge} ORDER BY ngh.id");
    }

    public function getIndicator($dataClassWeights){
        $normalisedWeights = $this->normaliseValues($dataClassWeights);

        $neighbourhoods = $this->db->launchQuery('SELECT * FROM neighbourhood');
        // We want to have an array indexed by neighbourhood_id
        foreach ($neighbourhoods as $neighbourhood) $aux[$neighbourhood['id']]=$neighbourhood;
        $neighbourhoods = $aux;

        foreach ($dataClassWeights as $dataclass_id=>$dataclass_weight) {
            $rawValues=null; 
            $neighbourhoodValues = $this->getNeighbourhoodValues($dataclass_id);
           
            foreach ($neighbourhoodValues as $value) $rawValues[$value['id']]=$value['_value'];
            $keys=array_keys($rawValues);
            foreach($neighbourhoods as $key=>$value) if (!in_array($key, $keys)) $noValues[]=$key;
            $maxAge = $this->db->launchQuery("SELECT MAX(dv.age) as age FROM district_value dv WHERE dv.dataclass_id={$dataclass_id}");
            $maxAge = $maxAge[0]['age'];
            foreach ($noValues as $idNeighbourhood){
                /*
                print("SELECT dis.*, dv._value, dv.age
                        FROM district dis JOIN district_value dv ON dv.district_id=dis.id AND
                                                                         dv.dataclass_id={$dataclass_id} AND
                                                                         dv.age={$maxAge} AND
                                                                         dis.id={$neighbourhoods[$idNeighbourhood]['district_id']}
                        ORDER BY dis.id"); print('<br>');*/
                $result= $this->db->launchQuery("SELECT dis.*, dv._value, dv.age
                        FROM district dis JOIN district_value dv ON dv.district_id=dis.id AND
                                                                    dv.dataclass_id={$dataclass_id} AND
                                                                    dv.age={$maxAge} AND
                                                                    dis.id={$neighbourhoods[$idNeighbourhood]['district_id']}
                        ORDER BY dis.id");
                $rawValues[$idNeighbourhood]=$result[0]['_value'];
            }
            $normalisedWeightedValues=$this->normaliseValues($rawValues, $normalisedWeights[$dataclass_id]);
            /*
            foreach ($normalisedWeightedValuesAux as $key=>$value) $normalisedWeightedValues[$key]+=$normalisedWeightedValuesAux[$key];
            print_r($normalisedWeightedValuesAux);print('<br>');
            print_r($normalisedWeightedValues);print('<br>');print('<br>');
             *
             */
            //print_r($normalisedWeightedValues);
            foreach ($neighbourhoods as $neighbourhood){
				//print_r($normalisedWeightedValues);
                if (!isset($normalisedWeightedValues[$neighbourhood['id']])) $normalisedWeightedValues[$neighbourhood['id']]=0;
				$value = $normalisedWeightedValues[$neighbourhood['id']];
				if($value >= 0.5)
				{
	                $colours[$neighbourhood['id']]['green'] += $value;
				}
				else
				{
	                $colours[$neighbourhood['id']]['red'] += $value;
				}
                // $colours[$neighbourhood['id']]['blue'] = '00';
            }
        }

foreach($colours as $color)
{
	$color['green'] = $color['green'] / count($dataClassWeights);
	$color['red'] = $color['red'] / count($dataClassWeights);
}

        //print_r($colours);
        return $colours;
    }



    private function normaliseValues($values, $weight=1.0){
        $aux = array_values($values);
        if (sort($aux, SORT_NUMERIC)){
            //print('<br>');print_r($aux);print('<br>');
            $min = $aux[0]; $max = $aux[count($aux)-1];
            $difference = $max - $min;
            // print($min.' '.$max.' '.$difference);
            foreach($values as $key=>$value){
                
                if ($difference>0)
					{
						$norm = 0;
						$norm = $value - $min;
						$norm = (float)($norm) / (float)($difference);
						$normalisedValues[$key] = $norm * $weight;
						// $normalisedValues[$key]= ((float)((float)($value-$min)/(float)$difference));
						// print('<br>'.$key.' '.$value.'  '. $norm);
					}
                else $normalisedValues[$key]=(float) 1/count($values);
            }
            //print('<br>Normalised:<br>');
            //print_r($normalisedValues);
			$sum = 0;
            foreach($normalisedValues as $value) $sum=max($sum,$value);
            //print('Sum '.$sum);
            return $normalisedValues;
        }
        else return null;
    }
}
?>