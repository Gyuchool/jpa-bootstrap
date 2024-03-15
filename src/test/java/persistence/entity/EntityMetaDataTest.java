package persistence.entity;

import domain.Person;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityMetaDataTest {

    @DisplayName("EntityMetaData의 정보가 다르면 true를 반환한다.")
    @Test
    void isDirtyWhenDifferentData(){
        Person person = new Person("KIM", "kim@test.com", 30);
        Person person1 = new Person("LEE", "kim@test.com", 20);

        EntityMetaData entityMetaData = new EntityMetaData(person);
        EntityMetaData entityMetaData2 = new EntityMetaData(person1);

        assertThat(entityMetaData.isDirty(entityMetaData2)).isTrue();
    }

    @DisplayName("EntityMetaData의 정보가 같으면 false를 반환한다.")
    @Test
    void isDirtyWhenSameData(){
        Person person = new Person("KIM", "kim@test.com", 30);

        EntityMetaData entityMetaData = new EntityMetaData(person);
        EntityMetaData entityMetaData2 = new EntityMetaData(person);

        assertThat(entityMetaData.isDirty(entityMetaData2)).isFalse();
    }

}
