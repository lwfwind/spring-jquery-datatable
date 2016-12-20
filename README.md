
# spring-jquery-datatable
This project is an extension of the Spring project to ease its use with jQuery plugin [DataTables](http://datatables.net/) with **server-side processing enabled**.

This will allow you to handle the Ajax requests sent by DataTables for each draw of the information on the page (i.e. when paging, ordering, searching, etc.) from Spring **@Controller**.

## Example:
#### On the server-side
```java
@Controller
@RequestMapping(value = "/pc")
public class PCController {

    @Autowired
    private PCActionService pcActionService;

    @RequestMapping(value = "/get_pc_tea_api_action")
    @ResponseBody
    public DatatablesResponse<PCTeaAPIAction> findPCTeaAPIActionsWithDatatablesCriterias(HttpServletRequest request) {
        DatatablesCriterias criterias = DatatablesCriterias.getFromRequest(request);
        DataSet<PCTeaAPIAction> actions = pcActionService.findPCTeaAPIActionsWithDatatablesCriterias(criterias);
        return DatatablesResponse.build(actions, criterias);
    }
}
```
```java
@Service
public class PCActionServiceImpl implements PCActionService {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DataSet<PCTeaAPIAction> findPCTeaAPIActionsWithDatatablesCriterias(DatatablesCriterias criterias) {
        TableQuery query = new TableQuery(entityManager, PCTeaAPIAction.class, criterias);
        return query.getResultDataSet();
    }

}
```

#### On the client-side

On the client-side, you can now define your table loading data dynamically :

```javascript
$(document).ready(function() {
    var table = $('#teaPCApiActionTable').DataTable({
        processing: true,
        serverSide: true,
        columns: [
            {"data": "time"},
            {"data": "uid"},
            {"data": "api_name"},
            {"data": "request_type"},
            {"data": "parameters"},
            {"data": "response_idx"}
        ],
        ajax: {
            url: '/pc/get_pc_tea_api_action',
            type: 'GET'
        }
    });
}
```

## Maven dependency

```xml
<dependency>
    <groupId>com.github.lwfwind.web</groupId>
    <artifactId>spring-jquery-datatable</artifactId>
    <version>2.5</version>
</dependency>
```
