package persistence.sql.column;

import domain.Person;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistence.sql.dialect.MysqlDialect;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IdColumn을 제외한")
class ColumnsTest {

    @DisplayName("column들의 정의를 반환한다.")
    @Test
    void getColumnsDefinition() {
        // given
        Columns columns = new Columns(Person.class.getDeclaredFields());
        // when
        String result = columns.getColumnsDefinition(new MysqlDialect());

        // then
        assertThat(result).isEqualTo("nick_name varchar(255), old integer, email varchar(255) not null");
    }

    @DisplayName("column들의 이름을 반환한다.")
    @Test
    void getColumnNames() {
        // given
        Columns columns = new Columns(Person.class.getDeclaredFields());
        // when
        String result = columns.getColumnNames();

        // then
        assertThat(result).isEqualTo("nick_name, old, email");
    }

    @DisplayName("column들의 값을 비교하여 다르면 true를 반환한다.")
    @Test
    void isDirty() {
        // given
        Person person1 = new Person("KIM", "kim@test.com", 30);
        Person person2 = new Person("LEE", "kim@test.com", 20);
        Columns columns = new Columns(person1);
        Columns columns2 = new Columns(person2);

        //when
        boolean result = columns.isDirty(columns2);

        //then
        assertThat(result).isTrue();
    }

    @DisplayName("column들의 값을 비교하여 같으면 false를 반환한다.")
    @Test
    void isDirtyWhenSameData() {
        // given
        Person person = new Person("KIM", "kim@test.com", 30);
        Columns columns = new Columns(person);
        Columns columns2 = new Columns(person);

        //when
        boolean result = columns.isDirty(columns2);

        //then
        assertThat(result).isFalse();
    }

}

